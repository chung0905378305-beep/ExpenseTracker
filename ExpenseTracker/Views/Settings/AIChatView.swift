import SwiftUI
import SwiftData

struct AIChatView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var context

    @Query(sort: \AIConversation.createdAt, order: .reverse)
    private var conversations: [AIConversation]

    @State private var selectedConversation: AIConversation?
    @State private var inputText = ""
    @State private var isLoading = false

    @State private var messages: [ChatMessage] = []

    // Config 默认值
    @State private var apiKey = ""
    @State private var baseURL = "https://api.openai.com"
    @State private var model = "gpt-4o"

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // 消息列表
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(spacing: 12) {
                            if messages.isEmpty {
                                welcomeView
                            }

                            ForEach(messages) { msg in
                                ChatBubble(message: msg)
                            }

                            if isLoading {
                                HStack {
                                    ProgressView()
                                        .padding()
                                    Spacer()
                                }
                            }

                            Color.clear.frame(height: 1).id("bottom")
                        }
                        .padding()
                    }
                    .onChange(of: messages.count) { _, _ in
                        withAnimation { proxy.scrollTo("bottom") }
                    }
                }

                // 输入区域
                HStack(spacing: 8) {
                    TextField("输入问题...", text: $inputText)
                        .textFieldStyle(.roundedBorder)
                        .disabled(isLoading)

                    Button {
                        sendMessage()
                    } label: {
                        Image(systemName: "arrow.up.circle.fill")
                            .font(.title2)
                            .foregroundColor(inputText.isEmpty ? .gray : .blue)
                    }
                    .disabled(inputText.isEmpty || isLoading)
                }
                .padding()

                // 免责声明
                Text("AI 分析仅供参考，不构成投资建议。数据仅保存在本地。")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                    .padding(.horizontal)
                    .padding(.bottom, 8)
            }
            .navigationTitle("AI 财务分析")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("关闭") { dismiss() }
                }
                ToolbarItem(placement: .primaryAction) {
                    Menu {
                        Button("生成月度报告", action: generateReport)
                        Button("清空对话", role: .destructive, action: { messages = [] })
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
        }
    }

    private var welcomeView: some View {
        VStack(spacing: 16) {
            Image(systemName: "brain.head.profile")
                .font(.system(size: 48))
                .foregroundColor(.blue)
            Text("AI 财务助手")
                .font(.title2.weight(.semibold))
            Text("我可以帮你分析收支、评估预算、发现储蓄机会。试试问我：")
                .font(.callout)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
            VStack(spacing: 8) {
                suggestionButton("分析我这个月的支出结构")
                suggestionButton("如何节省日常开支？")
                suggestionButton("我目前的储蓄率健康吗？")
            }
        }
        .padding(.vertical, 40)
    }

    private func suggestionButton(_ text: String) -> some View {
        Button(text) {
            inputText = text
            sendMessage()
        }
        .font(.subheadline)
        .foregroundColor(.blue)
        .buttonStyle(.bordered)
        .tint(.blue)
    }

    private func sendMessage() {
        guard !inputText.isEmpty else { return }
        let userMsg = ChatMessage(id: UUID(), role: "user", content: inputText, timestamp: Date())
        messages.append(userMsg)
        let query = inputText
        inputText = ""
        isLoading = true

        let key = apiKey.isEmpty ? AppSettings.default.aiAPIKey : apiKey
        let url = baseURL.isEmpty ? AppSettings.default.aiBaseURL : baseURL
        let mdl = model.isEmpty ? AppSettings.default.aiModel : model

        Task {
            do {
                let reply = try await AIService.shared.chat(
                    apiKey: key,
                    baseURL: url,
                    model: mdl,
                    message: query
                )
                let aiMsg = ChatMessage(id: UUID(), role: "assistant", content: reply, timestamp: Date())
                await MainActor.run {
                    messages.append(aiMsg)
                    isLoading = false
                }
            } catch {
                let errMsg = ChatMessage(id: UUID(), role: "assistant", content: "抱歉，请求失败：\(error.localizedDescription)\n请检查 AI 配置是否正确。", timestamp: Date())
                await MainActor.run {
                    messages.append(errMsg)
                    isLoading = false
                }
            }
        }
    }

    private func generateReport() {
        inputText = "请根据我的全部历史数据生成一份完整的月度财务报告"
        sendMessage()
    }
}

struct ChatBubble: View {
    let message: ChatMessage

    var body: some View {
        HStack(alignment: .top) {
            if message.role == "assistant" {
                Image(systemName: "brain.head.profile")
                    .font(.title3)
                    .foregroundColor(.blue)
                    .frame(width: 32)
            } else {
                Spacer()
            }

            Text(message.content)
                .font(.callout)
                .padding(12)
                .background(message.role == "user" ? .blue : .gray.opacity(0.12))
                .foregroundColor(message.role == "user" ? .white : .primary)
                .clipShape(RoundedRectangle(cornerRadius: 14))

            if message.role == "user" {
                Image(systemName: "person.circle.fill")
                    .font(.title3)
                    .foregroundColor(.blue)
                    .frame(width: 32)
            } else {
                Spacer()
            }
        }
    }
}
