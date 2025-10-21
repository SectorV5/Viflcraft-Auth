/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.command.commands.mail;

import win.sectorfive.viflcraftauth.common.AuthenticLibreLogin;
import win.sectorfive.viflcraftauth.common.command.Command;
import win.sectorfive.viflcraftauth.common.mail.AuthenticEMailHandler;

public class EMailCommand<P> extends Command<P> {

    protected final AuthenticEMailHandler mailHandler;

    public EMailCommand(AuthenticLibreLogin<P, ?> plugin) {
        super(plugin);
        mailHandler = plugin.getEmailHandler();
        assert mailHandler != null;
    }
}
