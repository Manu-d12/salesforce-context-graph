package org.autorabit.salesforcecontextgraph.ChatBot;


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

//    private static final String SALESFORCE_SYSTEM_PROMPT = """
//            You are an expert Salesforce Deployment Assistant specializing in metadata deployment issues and troubleshooting.
//
//            IMPORTANT RULES:
//            1. You ONLY answer questions related to Salesforce, Salesforce deployments, metadata, and Salesforce technologies.
//            2. You specialize in analyzing deployment failure messages and providing solutions.
//            3. For failed deployment messages, analyze the error, explain the issue, and provide step-by-step solutions.
//            4. If a user asks a question that is NOT related to Salesforce or deployments, respond with:
//               "I can only assist with Salesforce-related questions and deployment issues. Please ask me about your Salesforce metadata deployment problems."
//            5. Be helpful, detailed, and provide actionable guidance for resolving deployment issues.
//            6. If given a deployment error message, extract the component name, error type, and affected metadata, then provide solutions.
//            7. Consider common Salesforce deployment issues like: API version conflicts, missing dependencies, permission issues,
//               syntax errors in Apex/LWC/Flows, field dependencies, custom object references, etc.
//            """;

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
        this.client = client.defaultSystem(SYSTEM_PROMPT).build();
    }

    @GetMapping
    public String chat(@RequestParam("q") String message) {
        // Handle a general chat message
        try {
            return client.prompt().user(message).call().content();
        } catch (Exception e) {
            return "An error occurred while processing your message: " + e.getMessage();
        }
    }

    @PostMapping("/deployment-error")
    public String analyzeDeploymentError(@RequestBody DeploymentErrorRequest request) {
        // Analyze a Salesforce deployment error message
        try {
            String contextualPrompt = String.format("""
                    A Salesforce deployment has failed with the following error message:
                    
                    Error: %s
                    Component: %s
                    Deployment Status: %s
                    
                    """, request.errorMessage(), request.componentName(), request.deploymentStatus());

            return client.prompt().user(contextualPrompt).call().content();

        } catch (Exception e) {
            return "An error occurred while analyzing the deployment issue: " + e.getMessage();
        }
    }

}
