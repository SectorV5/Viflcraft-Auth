/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.command.commands.authorization;

import co.aikar.commands.annotation.*;
import net.kyori.adventure.audience.Audience;
import win.sectorfive.viflcraftauth.api.event.events.AuthenticatedEvent;
import win.sectorfive.viflcraftauth.api.event.events.WrongPasswordEvent.AuthenticationSource;
import win.sectorfive.viflcraftauth.common.AuthenticLibreLogin;
import win.sectorfive.viflcraftauth.common.command.InvalidCommandArgument;
import win.sectorfive.viflcraftauth.common.event.events.AuthenticWrongPasswordEvent;

import java.util.concurrent.CompletionStage;

@CommandAlias("login|l|log")
public class LoginCommand<P> extends AuthorizationCommand<P> {

    public LoginCommand(AuthenticLibreLogin<P, ?> premium) {
        super(premium);
    }

    @Default
    @Syntax("{@@syntax.login}")
    @CommandCompletion("%autocomplete.login")
    public CompletionStage<Void> onLogin(Audience sender, P player, @Single String password) {
        return runAsync(() -> {
            checkUnauthorized(player);
            var user = getUser(player);
            if (!user.isRegistered()) throw new InvalidCommandArgument(getMessage("error-not-registered"));

            sender.sendMessage(getMessage("info-logging-in"));

            var hashed = user.getHashedPassword();
            var crypto = getCrypto(hashed);

            if (crypto == null) throw new InvalidCommandArgument(getMessage("error-password-corrupted"));

            if (!crypto.matches(password, hashed)) {
                plugin.getEventProvider()
                        .unsafeFire(plugin.getEventTypes().wrongPassword,
                                new AuthenticWrongPasswordEvent<>(user, player, plugin, AuthenticationSource.LOGIN));
                throw new InvalidCommandArgument(getMessage("error-password-wrong"));
            }

            // Check if user is logging in with a RESET password
            if (password.startsWith("RESET:")) {
                sender.sendMessage(getMessage("info-reset-password-detected"));
                sender.sendMessage(getMessage("info-please-change-password"));
            }

            sender.sendMessage(getMessage("info-logged-in"));
            
            // Check if password is weak and warn the user
            if (plugin.isWeakPassword(password)) {
                var minLength = plugin.getConfiguration().get(win.sectorfive.viflcraftauth.common.config.ConfigurationKeys.MINIMUM_PASSWORD_LENGTH);
                sender.sendMessage(getMessage("info-weak-password-warning", "%min_length%", String.valueOf(minLength > 0 ? minLength : 8)));
            }
            
            getAuthorizationProvider().authorize(user, player, AuthenticatedEvent.AuthenticationReason.LOGIN);
        });
    }

}
