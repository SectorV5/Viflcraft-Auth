/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.command.commands.premium;

import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.Default;
import net.kyori.adventure.audience.Audience;
import win.sectorfive.viflcraftauth.common.AuthenticViflcraftAuth;

import java.util.concurrent.CompletionStage;

@CommandAlias("premiumconfirm|confirmpremium")
public class PremiumConfirmCommand<P> extends PremiumCommand<P> {
    public PremiumConfirmCommand(AuthenticViflcraftAuth<P, ?> plugin) {
        super(plugin);
    }

    @Default
    public CompletionStage<Void> onPremiumConfirm(Audience sender, P player) {
        return runAsync(() -> {
            var user = getUser(player);
            checkCracked(user);

            plugin.getCommandProvider().onConfirm(player, sender, user);
        });
    }

}
