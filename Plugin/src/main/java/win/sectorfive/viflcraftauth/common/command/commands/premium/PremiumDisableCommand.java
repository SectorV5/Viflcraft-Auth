/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.command.commands.premium;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import net.kyori.adventure.audience.Audience;
import win.sectorfive.viflcraftauth.common.AuthenticLibreLogin;
import win.sectorfive.viflcraftauth.common.event.events.AuthenticPremiumLoginSwitchEvent;

import java.util.concurrent.CompletionStage;

@CommandAlias("cracked|manuallogin")
public class PremiumDisableCommand<P> extends PremiumCommand<P> {

    public PremiumDisableCommand(AuthenticLibreLogin<P, ?> premium) {
        super(premium);
    }

    @Default
    public CompletionStage<Void> onCracked(Audience sender, P player) {
        return runAsync(() -> {
            var user = getUser(player);
            checkPremium(user);

            sender.sendMessage(getMessage("info-disabling"));

            user.setPremiumUUID(null);

            plugin.getEventProvider().unsafeFire(plugin.getEventTypes().premiumLoginSwitch, new AuthenticPremiumLoginSwitchEvent<>(user, player, plugin));

            getDatabaseProvider().updateUser(user);

            plugin.getPlatformHandle().kick(player, getMessage("kick-premium-info-disabled"));
        });
    }

}
