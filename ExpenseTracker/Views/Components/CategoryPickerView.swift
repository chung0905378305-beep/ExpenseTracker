import SwiftUI

struct CategoryPickerView: View {
    let categories: [Category]
    @Binding var selected: Category?
    let kind: TransactionKind

    private let columns = Array(repeating: GridItem(.flexible(), spacing: 8), count: 5)

    var body: some View {
        LazyVGrid(columns: columns, spacing: 12) {
            ForEach(categories) { cat in
                Button {
                    selected = cat
                } label: {
                    VStack(spacing: 6) {
                        Image(systemName: cat.icon)
                            .font(.title3)
                            .frame(width: 44, height: 44)
                            .background(
                                selected?.id == cat.id
                                ? .blue.opacity(0.15)
                                : .gray.opacity(0.08)
                            )
                            .overlay(
                                RoundedRectangle(cornerRadius: 10)
                                    .stroke(
                                        selected?.id == cat.id ? .blue : .clear,
                                        lineWidth: 2
                                    )
                            )
                            .clipShape(RoundedRectangle(cornerRadius: 10))

                        Text(cat.name)
                            .font(.system(size: 10))
                            .foregroundColor(selected?.id == cat.id ? .blue : .secondary)
                            .lineLimit(1)
                    }
                }
                .buttonStyle(.plain)
            }
        }
    }
}
