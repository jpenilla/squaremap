/**
 * @typedef {Object} Settings
 * @property {boolean} static
 * @property {Settings_World[]} worlds
 * @property {Settings_UI} ui
 */

/**
 * @typedef {Object} Settings_World
 * @property {string} name
 * @property {string} display_name
 * @property {string} icon
 * @property {string} type
 * @property {number} order
 */

/**
 * @typedef {Object} Settings_UI_Coordinates
 * @property {boolean} enabled
 * @property {string} html
 */

/**
 * @typedef {Object} Settings_UI_Link
 * @property {boolean} enabled
 */

/**
 * @typedef {Object} Settings_UI_Sidebar
 * @property {string} pinned
 * @property {string} player_list_label
 * @property {string} world_list_label
 */

/**
 * @typedef {Object} Settings_UI
 * @property {string} title
 * @property {Settings_UI_Coordinates} coordinates
 * @property {Settings_UI_Link} link
 * @property {Settings_UI_Sidebar} sidebar
 */

/**
 * @typedef {Object} WorldSettings_Spawn
 * @property {number} x
 * @property {number} z
 */

/**
 * @typedef {Object} WorldSettings_PlayerTracker_Nameplates
 * @property {boolean} enabled
 * @property {boolean} show_heads
 * @property {string} heads_url
 * @property {boolean} show_armor
 * @property {boolean} show_health
 */

/**
 * @typedef {Object} WorldSettings_PlayerTracker
 * @property {boolean} enabled
 * @property {number} update_interval
 * @property {string} label
 * @property {boolean} show_controls
 * @property {boolean} default_hidden
 * @property {number} priority
 * @property {number} z_index
 * @property {WorldSettings_PlayerTracker_Nameplates} nameplates
 */

/**
 * @typedef {Object} WorldSettings_Zoom
 * @property {number} max
 * @property {number} def
 * @property {number} extra
 */

/**
 * @typedef {Object} WorldSettings
 * @property {WorldSettings_Spawn} spawn
 * @property {WorldSettings_PlayerTracker} player_tracker
 * @property {WorldSettings_Zoom} zoom
 * @property {number} marker_update_interval
 * @property {number} tiles_update_interval
 */

/**
 * @typedef {Object} PlayerData
 * @property {string} name
 * @property {string} display_name
 * @property {string} uuid
 * @property {string} world
 * @property {number} x
 * @property {number} y
 * @property {number} z
 * @property {number} yaw
 * @property {number} armor
 * @property {number} health
 */

/**
 * @typedef {Object} PlayersData
 * @property {PlayerData[]} players
 * @property {number} max
 */
