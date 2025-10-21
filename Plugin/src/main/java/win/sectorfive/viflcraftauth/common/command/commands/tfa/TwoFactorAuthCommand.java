/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.command.commands.tfa;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import net.kyori.adventure.audience.Audience;
import win.sectorfive.viflcraftauth.common.AuthenticLibreLogin;
import win.sectorfive.viflcraftauth.common.command.Command;
import win.sectorfive.viflcraftauth.common.command.InvalidCommandArgument;
import win.sectorfive.viflcraftauth.common.config.ConfigurationKeys;

import java.util.concurrent.CompletionStage;

@CommandAlias("2fa|2fauth|2fauthcode")
public class TwoFactorAuthCommand<P> extends Command<P> {
    public TwoFactorAuthCommand(AuthenticLibreLogin<P, ?> plugin) {
        super(plugin);
    }

    @Default
    public CompletionStage<Void> onTwoFactorAuth(Audience sender, P player) {
        return runAsync(() -> {
            checkAuthorized(player);
            var user = getUser(player);
            var auth = plugin.getAuthorizationProvider();

            if (auth.isAwaiting2FA(player)) {
                throw new InvalidCommandArgument(getMessage("totp-show-info"));
            }

            if (!plugin.getImageProjector().canProject(player)) {
                throw new InvalidCommandArgument(getMessage("totp-wrong-version",
                        "%low%", "1.13",
                        "%high%", "1.21.1"
                ));
            }

            sender.sendMessage(getMessage("totp-generating"));

            var data = plugin.getTOTPProvider().generate(user);

            auth.beginTwoFactorAuth(user, player, data);

            plugin.cancelOnExit(plugin.delay(() -> {
                plugin.getImageProjector().project(data.qr(), player);

                sender.sendMessage(getMessage("totp-show-info"));
            }, plugin.getConfiguration().get(ConfigurationKeys.TOTP_DELAY)), player);
        });
    }
}
