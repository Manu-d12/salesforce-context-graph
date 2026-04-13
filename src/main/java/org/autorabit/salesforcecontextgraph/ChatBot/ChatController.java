package org.autorabit.salesforcecontextgraph.ChatBot;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.autorabit.salesforcecontextgraph.api.response.AnalysisGraphResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatClient client;

    private AnalysisGraphResponse analysisGraphResponse;

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

    public ChatController(ChatClient.Builder client) {
        this.client = client
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }

    @GetMapping
    public String chat(@RequestParam("q") String message) {
        // Handle a general chat message
        try {
            System.out.println(message);
            return client
                    .prompt()
                    .user(message)
                    .call()
                    .content();
        } catch (Exception e) {
            return "An error occurred while processing your message: " + e.getMessage();
        }
    }

    @PostMapping("/deployment-error")
    public String analyzeDeploymentError(@RequestBody DeploymentErrorRequest request) {
        try {
            String graphContext = buildGraphContext(request.graphResponse());

            String contextualPrompt = String.format("""
                            A Salesforce deployment has failed with the following error message:
                            
                            Error: %s
                            Component: %s
                            Deployment Status: %s
                            
                            DEPENDENCY GRAPH CONTEXT:
                            The following is a JSON representation of the metadata dependency graph
                            associated with this deployment. Each node represents a Salesforce metadata
                            component, and each edge represents a dependency between components.
                            
                            %s
                            
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
                    request.deploymentStatus(),
                    graphContext
            );

            return client.prompt().user(contextualPrompt).call().content();

        } catch (Exception e) {
            return "An error occurred while analyzing the deployment issue: " + e.getMessage();
        }
    }

    //serializing the graph
    private String buildGraphContext(AnalysisGraphResponse graph) {
        if (graph == null) {
            return "No dependency graph available.";
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(graph);
        } catch (JsonProcessingException e) {
            return "Graph context could not be serialized: " + e.getMessage();
        }
    }

}
