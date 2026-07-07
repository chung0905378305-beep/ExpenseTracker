import SwiftUI
import SwiftData

struct ProfileTab: View {
    @State private var showAIChat = false
    @State private var showThemePicker = false
    @State private var showCurrencyPicker = false
    @EnvironmentObject var appState: AppState

    // 会员信息
    @State private var memberInfo: MembershipService.MemberInfo?
    @State private var isLoggedIn = false

    var body: some View {
        NavigationStack {
            List {
                // 账户卡
                Section {
                    if isLoggedIn {
                        HStack(spacing: 14) {
                            Image(systemName: "person.circle.fill")
                                .font(.system(size: 48))
                                .foregroundColor(.blue)
                            VStack(alignment: .leading, spacing: 4) {
                                Text("用户")
                                    .font(.headline)
                                if let info = memberInfo {
                                    Text("\(info.planName) · 到期 \(info.expireAt.map { FormatHelper.day($0) } ?? "—")")
                                        .font(.caption)
                                        .foregroundColor(.blue)
                                }
                            }
                        }
                        .padding(.vertical, 4)
                        .listRowBackground(Color.blue.opacity(0.05))
                    } else {
                        NavigationLink(destination: LoginView()) {
                            HStack {
                                Image(systemName: "person.circle")
                                    .font(.system(size: 40))
                                    .foregroundColor(.secondary)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("登录 / 注册")
                                        .font(.headline)
                                    Text("同步数据到云端，解锁会员功能")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                        }
                        .listRowBackground(Color.blue.opacity(0.05))
                    }
                }

                // 会员订阅
                Section {
                    NavigationLink(destination: Text("会员订阅详情")) {
                        Label("会员订阅", systemImage: "crown.fill")
                    }
                    Button("升级到会员版") {}
                } header: {
                    Text("会员订阅").sectionHeader()
                }

                // 外观
                Section {
                    HStack {
                        Label("主题", systemImage: "paintpalette.fill")
                        Spacer()
                        Text(appState.theme.rawValue)
                            .foregroundColor(.secondary)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { showThemePicker = true }

                    Toggle(isOn: $appState.hideAmount) {
                        Label("隐藏金额", systemImage: "eye.slash.fill")
                    }
                } header: {
                    Text("外观").sectionHeader()
                }

                // 账户与安全
                Section {
                    Toggle(isOn: $appState.appLockEnabled) {
                        Label("App 锁", systemImage: "faceid")
                    }
                    if appState.appLockEnabled {
                        Picker("触发时机", selection: $appState.appLockMode) {
                            ForEach(AppLockMode.allCases, id: \.self) { mode in
                                Text(mode.description).tag(mode)
                            }
                        }
                    }

                    HStack {
                        Label("币种", systemImage: "yensign.circle")
                        Spacer()
                        Text(appState.baseCurrency)
                            .foregroundColor(.secondary)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture { showCurrencyPicker = true }
                } header: {
                    Text("账户与安全").sectionHeader()
                }

                // 数据与同步
                Section {
                    Label("iCloud 同步", systemImage: "icloud.fill")
                    NavigationLink(destination: Text("数据备份")) {
                        Label("备份与导入", systemImage: "arrow.triangle.2.circlepath")
                    }
                    NavigationLink(destination: Text("CSV导入")) {
                        Label("CSV 导入", systemImage: "doc.text.fill")
                    }
                } header: {
                    Text("数据与同步").sectionHeader()
                }

                // AI 与扩展
                Section {
                    Button {
                        showAIChat = true
                    } label: {
                        Label("AI 分析", systemImage: "brain.head.profile")
                    }
                    NavigationLink(destination: Text("邮箱绑定")) {
                        Label("邮箱自动记账", systemImage: "envelope.fill")
                    }
                    NavigationLink(destination: Text("行情源设置")) {
                        Label("行情源设置", systemImage: "antenna.radiowaves.left.and.right")
                    }
                } header: {
                    Text("AI 与扩展").sectionHeader()
                }

                // 分类/标签/账户管理
                Section {
                    NavigationLink(destination: CategoryManageView()) {
                        Label("分类管理", systemImage: "square.grid.2x2.fill")
                    }
                    NavigationLink(destination: TagManageView()) {
                        Label("标签管理", systemImage: "tag.fill")
                    }
                    NavigationLink(destination: AccountManageView()) {
                        Label("账户管理", systemImage: "building.columns.fill")
                    }
                } header: {
                    Text("数据管理").sectionHeader()
                }

                // 关于
                Section {
                    NavigationLink(destination: Text("隐私政策占位页")) {
                        Label("隐私政策", systemImage: "hand.raised.fill")
                    }
                    HStack {
                        Label("版本", systemImage: "info.circle")
                        Spacer()
                        Text("1.0.0")
                            .foregroundColor(.secondary)
                    }
                } header: {
                    Text("关于").sectionHeader()
                }
            }
            .navigationTitle("")
            .navigationBarHidden(true)
            .sheet(isPresented: $showAIChat) {
                AIChatView()
            }
        }
    }
}

// MARK: - Placeholder Views
struct LoginView: View {
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "person.circle")
                .font(.system(size: 60))
                .foregroundColor(.blue)
            Text("登录记账本账户")
                .font(.title2.weight(.semibold))

            TextField("邮箱", text: $email)
                .textFieldStyle(.roundedBorder)
                .keyboardType(.emailAddress)
                .padding(.horizontal, 32)

            SecureField("密码", text: $password)
                .textFieldStyle(.roundedBorder)
                .padding(.horizontal, 32)

            Button("登录") { }
                .buttonStyle(.borderedProminent)
                .padding(.top, 8)
        }
        .navigationTitle("登录")
    }
}

struct CategoryManageView: View {
    var body: some View { Text("分类管理").navigationTitle("分类管理") }
}
struct TagManageView: View {
    var body: some View { Text("标签管理").navigationTitle("标签管理") }
}
struct AccountManageView: View {
    var body: some View { Text("账户管理").navigationTitle("账户管理") }
}
