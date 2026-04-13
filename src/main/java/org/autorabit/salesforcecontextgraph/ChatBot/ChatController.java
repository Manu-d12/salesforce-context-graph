package org.autorabit.salesforcecontextgraph.ChatBot;


import org.autorabit.salesforcecontextgraph.service.OrgGraphContextStore;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient client;
    private OrgGraphContextStore orgGraphContextStore;
    private final ConcurrentMap<String, Long> injectedGraphVersionByConversation = new ConcurrentHashMap<>();
    private static final Set<String> GRAPH_QUERY_KEYWORDS = Set.of(
            "graph", "node", "edge", "dependency", "dependencies", "path", "impact", "related",
            "connected", "connection", "upstream", "downstream", "metadata", "component", "why failed"
    );

    private static final String SYSTEM_PROMPT = """
            You are a Salesforce expert AI assistant.
            
            Your responsibilities:
            - Answer any questions related to Salesforce, including:
            - Metadata deployments
            - Apex, SOQL, Lightning (LWC/Aura)
            - Admin configurations
            - CI/CD, SFDX, DevOps
            - Security, permissions, and best practices
            
            - Provide clear, accurate, and practical answers.
            
            ALLOWED GENERAL QUESTIONS:
            You are allowed to answer basic conversational questions such as:
            - "Who are you?"
            - "What can you do?"
            - "How can you help me?"
            
            For such questions, respond by explaining that you are a Salesforce-focused assistant.
            
            SPECIAL CAPABILITY:
            - If the user provides a Salesforce deployment error message or failure log:
            - Analyze the issue
            - Identify the root cause
            - Suggest actionable fixes
            - Structure the response as:
            1. Issue Summary
            2. Root Cause
            3. Suggested Fix
            4. Additional Tips (optional)
            
            STRICT RULES:
            1. You MUST only answer questions related to Salesforce.
            2. If the question is NOT related to Salesforce (except allowed general questions), respond EXACTLY with:
            "Sorry i can only answer questions related to salesforce."
            3. Do NOT provide partial answers for non-Salesforce topics.
            4. If a query is ambiguous, check if it relates to Salesforce before answering.
            
            GUIDELINES:
            - Be concise but helpful.
            - Prefer real-world examples and troubleshooting steps.
            - Do not hallucinate unknown Salesforce errors—ask for more details if needed.
            
            EXAMPLES OF VALID INPUT:
            - "What is a trigger in Salesforce?"
            - "Deployment failed with error: Missing dependent object"
            - "Who are you?"
            
            EXAMPLES OF INVALID INPUT:
            - "What is Java?"
            - "Explain operating systems"
            - "Write a Python script"
            
            For invalid inputs, respond ONLY with:
            "Sorry i can only answer questions related to salesforce."
            """;

    public ChatController(ChatClient.Builder client, OrgGraphContextStore orgGraphContextStore) {

        ChatMemory chatMemory = MessageWindowChatMemory.builder().maxMessages(50).build();
        this.orgGraphContextStore = orgGraphContextStore;

        this.client = client
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @GetMapping("/query")
    public String chat(@RequestParam("q") String message,
                       @RequestParam(value = "sfOrgId", required = false) String sfOrgId,
                       @RequestParam(value = "sessionId", required = false) String sessionId) {
        try {
            String conversationId = buildConversationId(sfOrgId, sessionId);
            boolean graphQuestion = isGraphQuestion(message);

            String promptToSend = message;
            if (graphQuestion) {
                var contextPayload = orgGraphContextStore.findGraphContext(sfOrgId);
                if (contextPayload.isPresent()) {
                    long currentVersion = contextPayload.get().version();
                    long injectedVersion = injectedGraphVersionByConversation.getOrDefault(conversationId, -1L);
                    if (currentVersion != injectedVersion) {
                        promptToSend = String.format("""
                                        User question:
                                        %s

                                        Graph context JSON for this org:
                                        %s

                                        Use this graph context for graph-specific reasoning.
                                        """,
                                message,
                                contextPayload.get().contextJson()
                        );
                        injectedGraphVersionByConversation.put(conversationId, currentVersion);
                    }
                } else {
                    promptToSend = String.format("""
                                    User question:
                                    %s

                                    No graph context exists yet for this orgId.
                                    If the user asks graph-specific questions, ask them to run /api/analysis/target first.
                                    """,
                            message
                    );
                }
            }

            return client
                    .prompt()
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(promptToSend)
                    .call()
                    .content();
        } catch (Exception e) {
            return "An error occurred while processing your message: " + e.getMessage();
        }
    }

    @GetMapping("/deployment-error")
    public String analyzeDeploymentError(@RequestBody DeploymentErrorRequest request) {
        try {

            String contextualPrompt = String.format("""
                            A Salesforce deployment has failed with the following error message:
                            
                            Error: %s
                            Component: %s
                            Deployment Status: %s
                            
                            DEPENDENCY GRAPH CONTEXT:
                            The following is a JSON representation of the metadata dependency graph
                            associated with this deployment. Each node represents a Salesforce metadata
                            component, and each edge represents a dependency between components.
                            
                            
                            Based on the error above AND the dependency graph:
                            1. Issue Summary
                            2. Root Cause
                            3. Identify the EXACT node(s) and/or edge(s) in the graph where the failure
                               is occurring or originating from (reference them by their id/name fields).
                            4. Suggested Fix
                            5. Additional Tips (optional)
                            """,
                    request.errorMessage(),
                    request.componentName(),
                    request.deploymentStatus()
            );

            return client.prompt().user(contextualPrompt).call().content();

        } catch (Exception e) {
            return "An error occurred while analyzing the deployment issue: " + e.getMessage();
        }
    }

    private String buildConversationId(String sfOrgId, String sessionId) {
        String orgPart = (sfOrgId == null || sfOrgId.isBlank()) ? "no-org" : sfOrgId.trim();
        String sessionPart = (sessionId == null || sessionId.isBlank()) ? "default-session" : sessionId.trim();
        return orgPart + "::" + sessionPart;
    }

    private boolean isGraphQuestion(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }
        String normalized = message.toLowerCase();
        for (String keyword : GRAPH_QUERY_KEYWORDS) {
            if (normalized.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

}
