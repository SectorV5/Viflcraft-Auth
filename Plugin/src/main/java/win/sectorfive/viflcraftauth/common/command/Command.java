/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.MessageKeys;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.TextComponent;
import win.sectorfive.viflcraftauth.api.Logger;
import win.sectorfive.viflcraftauth.api.configuration.Messages;
import win.sectorfive.viflcraftauth.api.crypto.CryptoProvider;
import win.sectorfive.viflcraftauth.api.crypto.HashedPassword;
import win.sectorfive.viflcraftauth.api.database.ReadWriteDatabaseProvider;
import win.sectorfive.viflcraftauth.api.database.User;
import win.sectorfive.viflcraftauth.common.AuthenticViflcraftAuth;
import win.sectorfive.viflcraftauth.common.authorization.AuthenticAuthorizationProvider;
import win.sectorfive.viflcraftauth.common.util.GeneralUtil;

import java.util.concurrent.CompletionStage;

public class Command<P> extends BaseCommand {

    protected final AuthenticViflcraftAuth<P, ?> plugin;

    public Command(AuthenticViflcraftAuth<P, ?> plugin) {
        this.plugin = plugin;
    }

    protected ReadWriteDatabaseProvider getDatabaseProvider() {
        return plugin.getDatabaseProvider();
    }

    protected Logger getLogger() {
        return plugin.getLogger();
    }

    protected Messages getMessages() {
        return plugin.getMessages();
    }

    protected TextComponent getMessage(String key, String... replacements) {
        return getMessages().getMessage(key, replacements);
    }

    protected AuthenticAuthorizationProvider<P, ?> getAuthorizationProvider() {
        return plugin.getAuthorizationProvider();
    }

    protected void checkAuthorized(P player) {
        if (!getAuthorizationProvider().isAuthorized(player)) {
            throw new InvalidCommandArgument(getMessage("error-not-authorized"));
        }
    }

    protected CryptoProvider getCrypto(HashedPassword password) {
        return plugin.getCryptoProvider(password.algo());
    }

    public CompletionStage<Void> runAsync(Runnable runnable) {
        return GeneralUtil.runAsync(runnable);
    }

    protected User getUser(P player) {
        if (player == null)
            throw new co.aikar.commands.InvalidCommandArgument(MessageKeys.NOT_ALLOWED_ON_CONSOLE, false);

        var uuid = plugin.getPlatformHandle().getUUIDForPlayer(player);

        if (plugin.fromFloodgate(uuid)) throw new InvalidCommandArgument(getMessage("error-from-floodgate"));

        return plugin.getDatabaseProvider().getByUUID(uuid);
    }

    protected void setPassword(Audience sender, User user, String password, String messageKey) {
        // Validate password length first
        var minLength = plugin.getConfiguration().get(win.sectorfive.viflcraftauth.common.config.ConfigurationKeys.MINIMUM_PASSWORD_LENGTH);
        if (minLength > 0 && password.length() < minLength) {
            throw new InvalidCommandArgument(getMessage("error-password-too-short", 
                "%min_length%", String.valueOf(minLength),
                "%actual_length%", String.valueOf(password.length())));
        }
        
        // Validate password and get detailed error message
        String validationError = plugin.validatePassword(password);
        if (validationError != null) {
            throw new InvalidCommandArgument(getMessage(validationError));
        }

        sender.sendMessage(getMessage(messageKey));

        var defaultProvider = plugin.getDefaultCryptoProvider();

        var hash = defaultProvider.createHash(password);

        if (hash == null) {
            throw new InvalidCommandArgument(getMessage("error-password-too-long"));
        }

        user.setHashedPassword(hash);
    }

}
