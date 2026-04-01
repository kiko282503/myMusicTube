import SwiftUI
import ComposeApp

@main
struct iOSApp: App {
    init() {
        // initKoin() → doInitKoin() in Swift (Kotlin/Native strips "init" prefix)
        MainViewControllerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .ignoresSafeArea(.keyboard)
        }
    }
}
