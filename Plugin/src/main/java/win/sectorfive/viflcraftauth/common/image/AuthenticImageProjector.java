/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.common.image;

import win.sectorfive.viflcraftauth.api.image.ImageProjector;
import win.sectorfive.viflcraftauth.common.AuthenticHandler;
import win.sectorfive.viflcraftauth.common.AuthenticViflcraftAuth;

public abstract class AuthenticImageProjector<P, S> extends AuthenticHandler<P, S> implements ImageProjector<P> {

    public AuthenticImageProjector(AuthenticViflcraftAuth<P, S> plugin) {
        super(plugin);
    }

    public abstract void enable();

}
