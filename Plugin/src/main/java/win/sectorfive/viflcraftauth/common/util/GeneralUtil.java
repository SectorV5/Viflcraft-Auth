/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.util;

import net.kyori.adventure.text.TextComponent;
import org.jetbrains.annotations.Nullable;
import win.sectorfive.viflcraftauth.api.Logger;
import win.sectorfive.viflcraftauth.api.database.ReadDatabaseProvider;
import win.sectorfive.viflcraftauth.api.database.connector.DatabaseConnector;
import win.sectorfive.viflcraftauth.common.AuthenticLibreLogin;
import win.sectorfive.viflcraftauth.common.command.InvalidCommandArgument;
import win.sectorfive.viflcraftauth.common.config.ConfigurationKeys;
import win.sectorfive.viflcraftauth.common.config.HoconPluginConfiguration;
import win.sectorfive.viflcraftauth.common.config.key.ConfigurationKey;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ForkJoinPool;

// Migration imports removed
// import static win.sectorfive.viflcraftauth.common.config.ConfigurationKeys.MIGRATION_TYPE;
import static win.sectorfive.viflcraftauth.common.config.ConfigurationKeys.DATABASE_TYPE;

public class GeneralUtil {

    public static final ForkJoinPool ASYNC_POOL = new ForkJoinPool(4);

    public static String readInput(InputStream inputStream) throws IOException {
        var input = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        inputStream.close();
        return input;
    }

    public static Throwable getFurthestCause(Throwable throwable) {
        while (true) {
            var cause = throwable.getCause();

            if (cause == null) return throwable;

            throwable = cause;
        }
    }

    public static UUID fromUnDashedUUID(String id) {
        return id == null ? null : new UUID(
                new BigInteger(id.substring(0, 16), 16).longValue(),
                new BigInteger(id.substring(16, 32), 16).longValue()
        );
    }

    @Nullable
    public static TextComponent formatComponent(@Nullable TextComponent component, Map<String, String> replacements) {
        if (component == null) return null;

        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            component = (TextComponent) component.replaceText(builder -> builder.matchLiteral(entry.getKey()).replacement(entry.getValue()));
        }
        return component;
    }

    public static UUID getCrackedUUIDFromName(String name) {
        if (name == null) return null;
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    // Data migration feature removed
    /*
    public static void checkAndMigrate(HoconPluginConfiguration configuration, Logger logger, AuthenticLibreLogin<?, ?> plugin) {
        // Migration feature has been removed from Viflcraft Auth
    }
    */

    public static CompletionStage<Void> runAsync(Runnable runnable) {
        var future = new CompletableFuture<Void>();
        AuthenticLibreLogin.EXECUTOR.submit(() -> {
            try {
                runnable.run();
                future.complete(null);
            } catch (InvalidCommandArgument e) {
                future.completeExceptionally(e);
            } catch (Throwable e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    public static List<ConfigurationKey<?>> extractKeys(Class<?> clazz) {
        var list = new ArrayList<ConfigurationKey<?>>();
        try {
            for (Field field : clazz.getFields()) {
                if (field.getType() != ConfigurationKey.class) continue;
                list.add((ConfigurationKey<?>) field.get(null));

            }
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        return list;
    }

    public static String generateAlphanumericText(int limit) {
        var leftLimit = 48; // numeral '0'
        var rightLimit = 122; // letter 'z'
        var random = new SecureRandom();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(limit)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static Field getFieldByType(Class<?> clazz, Class<?> fieldType) {
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType().equals(fieldType)) {
                return field;
            }
        }
        return null;
    }

}
