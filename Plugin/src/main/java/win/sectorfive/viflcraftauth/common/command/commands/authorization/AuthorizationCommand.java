/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.command.commands.authorization;

import win.sectorfive.viflcraftauth.common.AuthenticViflcraftAuth;
import win.sectorfive.viflcraftauth.common.command.Command;
import win.sectorfive.viflcraftauth.common.command.InvalidCommandArgument;

public class AuthorizationCommand<P> extends Command<P> {

    public AuthorizationCommand(AuthenticViflcraftAuth<P, ?> premium) {
        super(premium);
    }

    protected void checkUnauthorized(P player) {
        if (getAuthorizationProvider().isAuthorized(player)) {
            throw new InvalidCommandArgument(getMessage("error-already-authorized"));
        }
    }

}
