/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package win.sectorfive.viflcraftauth.api.event;

import win.sectorfive.viflcraftauth.api.LibreLoginPlugin;
import win.sectorfive.viflcraftauth.api.PlatformHandle;

/**
 * An abstract event for all events
 *
 * @author kyngs
 */
public interface Event<P, S> {

    /**
     * Gets the plugin instance
     *
     * @return the plugin instance
     */
    LibreLoginPlugin<P, S> getPlugin();

    /**
     * Gets the platform handle
     *
     * @return the platform handle
     */
    PlatformHandle<P, S> getPlatformHandle();

}
