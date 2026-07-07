import SwiftUI
import SwiftData
import BackgroundTasks
import CloudKit

@main
struct ExpenseTrackerApp: App {
    @StateObject private var appState = AppState.shared
    @State private var showLock = false

    let container: ModelContainer

    init() {
        do {
            let schema = Schema([
                Transaction.self, Category.self, Tag.self, Account.self,
                Budget.self, RecurringRule.self, Subscription.self,
                Holding.self, HoldingSnapshot.self, NetWorthSnapshot.self,
                AIConversation.self, AppSettings.self
            ])

            #if targetEnvironment(simulator)
            let config = ModelConfiguration(schema: schema, isStoredInMemoryOnly: false)
            #else
            let config = ModelConfiguration(
                schema: schema,
                cloudKitContainer: CKContainer(identifier: "iCloud.com.expensetracker.app")
            )
            #endif

            container = try ModelContainer(for: schema, configurations: [config])
        } catch {
            fatalError("ModelContainer 初始化失败: \(error)")
        }

        BGTaskScheduler.shared.register(forTaskWithIdentifier: "com.expensetracker.quote-refresh", using: nil) { task in
            Task { await MarketService.shared.handleBackgroundRefresh(task as! BGAppRefreshTask) }
        }
        BGTaskScheduler.shared.register(forTaskWithIdentifier: "com.expensetracker.daily-snapshot", using: nil) { task in
            Task { await AutoGenerator.shared.handleDailySnapshot(task as! BGProcessingTask) }
        }
    }

    var body: some Scene {
        WindowGroup {
            Group {
                if showLock && AppState.shared.appLockEnabled {
                    AppLockView(onUnlock: { showLock = false })
                } else {
                    ContentView()
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.willResignActiveNotification)) { _ in
                if AppState.shared.appLockMode == .background {
                    showLock = true
                }
            }
            .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
                if AppState.shared.appLockMode == .coldStart {
                    showLock = true
                }
            }
            .task {
                await SeedData.seedIfNeeded(context: container.mainContext)
            }
        }
        .modelContainer(container)
        .environmentObject(appState)
    }
}
