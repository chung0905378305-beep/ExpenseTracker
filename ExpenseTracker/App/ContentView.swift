import SwiftUI

struct ContentView: View {
    @State private var selectedTab = 0

    var body: some View {
        ZStack(alignment: .bottom) {
            TabView(selection: $selectedTab) {
                TransactionsTab()
                    .tabItem {
                        Label("明细", systemImage: "list.bullet.rectangle")
                    }
                    .tag(0)

                StatsTab()
                    .tabItem {
                        Label("统计", systemImage: "chart.bar.fill")
                    }
                    .tag(1)

                AssetsTab()
                    .tabItem {
                        Label("资产", systemImage: "building.columns.fill")
                    }
                    .tag(2)

                ProfileTab()
                    .tabItem {
                        Label("我的", systemImage: "person.fill")
                    }
                    .tag(3)
            }

            if selectedTab == 0 {
                VStack {
                    Spacer()
                    HStack {
                        Spacer()
                        AddTransactionFAB()
                        Spacer()
                    }
                    .padding(.bottom, 90)
                }
                .allowsHitTesting(true)
            }
        }
        .tint(.accentColor)
    }
}

struct AddTransactionFAB: View {
    @State private var showSheet = false

    var body: some View {
        Button {
            showSheet = true
        } label: {
            Image(systemName: "plus")
                .font(.title2.weight(.semibold))
                .foregroundColor(.white)
                .frame(width: 56, height: 56)
                .background(
                    Circle()
                        .fill(.blue.gradient)
                        .shadow(color: .blue.opacity(0.35), radius: 8, y: 4)
                )
        }
        .sheet(isPresented: $showSheet) {
            AddTransactionSheet()
        }
    }
}
