package io.bitdive.mcpserver.utils;

import io.bitdive.mcpserver.config.securety.dto.CurrentUser;

public class UserViewModule {
    public static boolean isViewForModule(CurrentUser currentUser, String moduleName) {
        return currentUser.isAdmin() || currentUser.moduleView().stream()
                .anyMatch(userModuleView -> userModuleView.equalsIgnoreCase(
                                moduleName.contains("-")
                                        ? moduleName.substring(0, moduleName.toLowerCase().indexOf("-"))
                                        : moduleName
                        )
                );
    }
}
