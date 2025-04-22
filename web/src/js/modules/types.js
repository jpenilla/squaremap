/**
 * @typedef {Object} Settings
 * @property {boolean} static
 * @property {Settings.World[]} worlds
 * @property {Settings.UI} ui
 */

/**
 * @typedef {Object} Settings.World
 * @property {string} name
 * @property {string} display_name
 * @property {string} icon
 * @property {string} type
 * @property {number} order
 */

/**
 * @typedef {Object} Settings.UI.Coordinates
 * @property {boolean} enabled
 * @property {string} html
 */

/**
 * @typedef {Object} Settings.UI.Link
 * @property {boolean} enabled
 */

/**
 * @typedef {Object} Settings.UI.Sidebar
 * @property {string} pinned
 * @property {string} player_list_label
 * @property {string} world_list_label
 */

/**
 * @typedef {Object} Settings.UI
 * @property {string} title
 * @property {Settings.UI.Coordinates} coordinates
 * @property {Settings.UI.Link} link
 * @property {Settings.UI.Sidebar} sidebar
 */

/**
 * @typedef {Object} WorldSettings.Spawn
 * @property {number} x
 * @property {number} z
 */

/**
 * @typedef {Object} WorldSettings.PlayerTracker.Nameplates
 * @property {boolean} enabled
 * @property {boolean} show_heads
 * @property {string} heads_url
 * @property {boolean} show_armor
 * @property {boolean} show_health
 */

/**
 * @typedef {Object} WorldSettings.PlayerTracker
 * @property {boolean} enabled
 * @property {number} update_interval
 * @property {string} label
 * @property {boolean} show_controls
 * @property {boolean} default_hidden
 * @property {number} priority
 * @property {number} z_index
 * @property {WorldSettings.PlayerTracker.Nameplates} nameplates
 */

/**
 * @typedef {Object} WorldSettings.Zoom
 * @property {number} max
 * @property {number} def
 * @property {number} extra
 */

/**
 * @typedef {Object} WorldSettings
 * @property {WorldSettings.Spawn} spawn
 * @property {WorldSettings.PlayerTracker} player_tracker
 * @property {WorldSettings.Zoom} zoom
 * @property {number} marker_update_interval
 * @property {number} tiles_update_interval
 */
