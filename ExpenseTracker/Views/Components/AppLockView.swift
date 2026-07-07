import SwiftUI
import LocalAuthentication

struct AppLockView: View {
    let onUnlock: () -> Void

    @State private var isUnlocking = false
    @State private var errorMessage: String?
    @State private var password = ""

    var body: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "lock.shield.fill")
                .font(.system(size: 64))
                .foregroundColor(.blue)

            Text("记账本已锁定")
                .font(.title2.weight(.semibold))

            if let error = errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(.red)
            }

            // Biometric
            let biometryType = LAContext().biometryType
            if biometryType != .none {
                Button {
                    authenticateWithBiometrics()
                } label: {
                    HStack {
                        Image(systemName: biometryType == .faceID ? "faceid" : "touchid")
                            .font(.title2)
                        Text(biometryType == .faceID ? "使用面容 ID" : "使用指纹")
                    }
                    .padding(.horizontal, 32)
                    .padding(.vertical, 14)
                    .background(.blue)
                    .foregroundColor(.white)
                    .clipShape(Capsule())
                }
            }

            // 密码兜底
            VStack(spacing: 8) {
                SecureField("输入密码", text: $password)
                    .textFieldStyle(.roundedBorder)
                    .frame(width: 220)
                Button("密码解锁") {
                    // 简单密码校验占位
                    onUnlock()
                }
                .font(.callout)
            }
            .padding(.top, 12)

            Spacer()
        }
        .padding()
        .onAppear {
            authenticateWithBiometrics()
        }
    }

    private func authenticateWithBiometrics() {
        let context = LAContext()
        var error: NSError?

        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            return
        }

        isUnlocking = true
        context.evaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, localizedReason: "解锁记账本") { success, _ in
            DispatchQueue.main.async {
                isUnlocking = false
                if success {
                    onUnlock()
                } else {
                    errorMessage = "验证失败"
                }
            }
        }
    }
}
