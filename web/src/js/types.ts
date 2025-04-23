export interface Settings {
    static: boolean;
    worlds: Settings_World[];
    ui: Settings_UI;
}

export interface Settings_World {
    name: string;
    display_name: string;
    icon: string;
    type: string;
    order: number;
}

export interface Settings_UI_Coordinates {
    enabled: boolean;
    html: string;
}

export interface Settings_UI_Link {
    enabled: boolean;
}

export interface Settings_UI_Sidebar {
    pinned: string;
    player_list_label: string;
    world_list_label: string;
}

export interface Settings_UI {
    title: string;
    coordinates: Settings_UI_Coordinates;
    link: Settings_UI_Link;
    sidebar: Settings_UI_Sidebar;
}

export interface WorldSettings_Spawn {
    x: number;
    z: number;
}

export interface WorldSettings_PlayerTracker_Nameplates {
    enabled: boolean;
    show_heads: boolean;
    heads_url: string;
    show_armor: boolean;
    show_health: boolean;
}

export interface WorldSettings_PlayerTracker {
    enabled: boolean;
    update_interval: number;
    label: string;
    show_controls: boolean;
    default_hidden: boolean;
    priority: number;
    z_index: number;
    nameplates: WorldSettings_PlayerTracker_Nameplates;
}

export interface WorldSettings_Zoom {
    max: number;
    def: number;
    extra: number;
}

export interface WorldSettings {
    spawn: WorldSettings_Spawn;
    player_tracker: WorldSettings_PlayerTracker;
    zoom: WorldSettings_Zoom;
    marker_update_interval: number;
    tiles_update_interval: number;
}

export interface PlayerData {
    name: string;
    display_name: string;
    uuid: string;
    world: string;
    x: number;
    y: number;
    z: number;
    yaw: number;
    armor: number;
    health: number;
}

export interface PlayersData {
    players: PlayerData[];
    max: number;
}
