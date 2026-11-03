package dev.sqwat.vela.core;

/**
 * Preset deflate dictionary for share keys: the JSON names and pieces a config is made of, the most common last.
 * Frozen: keys already handed out are read with exactly these bytes. New modules need no change here (they only
 * compress a little less); a different dictionary needs a new key prefix in {@link ConfigShare}.
 */
final class ConfigDictionary {
    private ConfigDictionary(){}
    static final byte[] V5=(""
        +"\"uiScale\":\"replaceMainMenu\":\"reducedMotion\":\"animations\":\"alwaysShowBinds\":\"panelModuleMenu\":\"uiSounds\":\"uiSou"
        +"ndVolume\":\"uiSoundPreset\":\"language\":\"chatPrivacy\":\"selectedTheme\":\"friendVisuals\":\"protectFriends\":\"friendChi"
        +"naHat\":\"friendDashTrail\":\"friendJumpCircle\":\"friendFavoritesFirst\":\"favorites\":\"searchBind\":\"openModifiers\":\"o"
        +"ptimization\":\"accent\":\"age\":\"alias\":\"armor_percent\":\"automatic\":\"bps\":\"by_height\":\"caption\":\"cells\":\"chat\":\"ch"
        +"ina_hat\":\"color2\":\"color_mode\":\"containers\":\"coordinate_links\":\"cps\":\"custom_items\":\"damage\":\"dash_trail\":\"dea"
        +"ths\":\"debris\":\"delay_ms\":\"detail\":\"diag_text_cache\":\"display_cap_opt_in\":\"dot\":\"dynamic\":\"edit_items\":\"egg\":\"e"
        +"quipment_percent\":\"experience\":\"fill\":\"first_person\":\"flight\":\"fog\":\"fog_color\":\"fog_culling\":\"fog_density\":\"f"
        +"og_falloff\":\"fog_haze\":\"fog_height\":\"fog_theme\":\"forget_world\":\"format\":\"fps\":\"friend_color\":\"friends\":\"friend"
        +"s_only\":\"gap\":\"gravity\":\"hand\":\"hand_mode\":\"hands\":\"healing\":\"health\":\"health_hp\":\"height\":\"history\":\"hits\":\"h"
        +"otbar\":\"hour\":\"inspect_key\":\"inventory\":\"jump_circle\":\"jumps\":\"keep\":\"key\":\"knockback\":\"landings\":\"library\":\"l"
        +"ibrary_key\":\"lifetime\":\"lookup_caches\":\"losses\":\"lyrics\":\"lyrics_client_color\":\"lyrics_color\":\"mention_color\":"
        +"\"mention_sound\":\"mentions\":\"micro_opts\":\"mini\":\"motion\":\"mouse\":\"mute_vanilla\":\"names\":\"nested\":\"occlusion_han"
        +"ging\":\"occlusion_holograms\":\"occlusion_mobs\":\"occlusion_players\":\"off_\":\"offset_x\":\"offset_y\":\"offset_z\":\"onli"
        +"ne\":\"only_target\":\"open_folder\":\"orbit\":\"orbit_speed\":\"other_dimension\":\"others\":\"outline\":\"own_color\":\"pearl\""
        +":\"pin_key\":\"ping\":\"players\":\"potion\":\"preview\":\"range\":\"ratio\":\"remember\":\"reply_command\":\"roll\":\"saturation\":"
        +"\"screen_warning\":\"scroller\":\"search\":\"search_combo\":\"second_color\":\"self\":\"shader_editor\":\"shrink\":\"skin\":\"sky"
        +"_brightness\":\"sky_client_color\":\"sky_mode\":\"sky_quality\":\"sky_theme\":\"smart\":\"snowball\":\"space\":\"spawners\":\"sp"
        +"eed\":\"spread\":\"sprint_only\":\"stack\":\"stun\":\"swap_key\":\"sweep_key\":\"tab\":\"target\":\"thickness\":\"threads\":\"time_c"
        +"olor\":\"timestamps\":\"tint_strength\":\"totals\":\"totems\":\"tps\":\"vaults\":\"visibility\":\"warp\":\"warp_strength\":\"wave_"
        +"radius\":\"weather\":\"wet\":\"wet_amount\":\"wet_quality\":\"wet_reflection\":\"width\":\"wind\":\"window\":\"yaw\":\"armor\":\"cou"
        +"nt\":\"delay\":\"editor\":\"equipment\":\"open_key\":\"pitch\":\"respawn\":\"scale\":\"seconds\":\"shape\":\"sky\":\"sky_color\":\"sou"
        +"nd\":\"third_person\":\"tint\":\"volume\":\"distance\":\"mobs\":\"strength\":\"time\":\"transparent\":\"opacity\":\"radius\":\"size\""
        +":\"duration\":\"mode\":\"client_color\":\"color\":\"ambience\":{\"enabled\":true,\"settings\":{\"animations\":{\"enabled\":true,"
        +"\"settings\":{\"armor\":{\"enabled\":true,\"settings\":{\"aspect_ratio\":{\"enabled\":true,\"settings\":{\"auto_reconnect\":{\""
        +"enabled\":true,\"settings\":{\"auto_tpaccept\":{\"enabled\":true,\"settings\":{\"autosprint\":{\"enabled\":true,\"settings\":"
        +"{\"block_overlay\":{\"enabled\":true,\"settings\":{\"brew_viewer\":{\"enabled\":true,\"settings\":{\"chat\":{\"enabled\":true,"
        +"\"settings\":{\"china_hat\":{\"enabled\":true,\"settings\":{\"click_pearl\":{\"enabled\":true,\"settings\":{\"container_tools"
        +"\":{\"enabled\":true,\"settings\":{\"cooldowns\":{\"enabled\":true,\"settings\":{\"coordinates\":{\"enabled\":true,\"settings\""
        +":{\"crosshair\":{\"enabled\":true,\"settings\":{\"crystal_optimizer\":{\"enabled\":true,\"settings\":{\"custom_hotbar\":{\"en"
        +"abled\":true,\"settings\":{\"dash_trail\":{\"enabled\":true,\"settings\":{\"death\":{\"enabled\":true,\"settings\":{\"death_re"
        +"cap\":{\"enabled\":true,\"settings\":{\"explosion_wave\":{\"enabled\":true,\"settings\":{\"fake_player\":{\"enabled\":true,\"s"
        +"ettings\":{\"far\":{\"enabled\":true,\"settings\":{\"frame_generation\":{\"enabled\":true,\"settings\":{\"freelook\":{\"enable"
        +"d\":true,\"settings\":{\"gamma\":{\"enabled\":true,\"settings\":{\"hand_swap\":{\"enabled\":true,\"settings\":{\"hit_bubbles\":"
        +"{\"enabled\":true,\"settings\":{\"hit_color\":{\"enabled\":true,\"settings\":{\"hit_sound\":{\"enabled\":true,\"settings\":{\"h"
        +"itboxes\":{\"enabled\":true,\"settings\":{\"hotkeys\":{\"enabled\":true,\"settings\":{\"hp_counter\":{\"enabled\":true,\"setti"
        +"ngs\":{\"inventory\":{\"enabled\":true,\"settings\":{\"item_highlight\":{\"enabled\":true,\"settings\":{\"item_physics\":{\"en"
        +"abled\":true,\"settings\":{\"jump_circle\":{\"enabled\":true,\"settings\":{\"keystrokes\":{\"enabled\":true,\"settings\":{\"ki"
        +"ll_effect\":{\"enabled\":true,\"settings\":{\"loot_memory\":{\"enabled\":true,\"settings\":{\"mace_smash\":{\"enabled\":true,"
        +"\"settings\":{\"module_list\":{\"enabled\":true,\"settings\":{\"motion_blur\":{\"enabled\":true,\"settings\":{\"music\":{\"enab"
        +"led\":true,\"settings\":{\"near\":{\"enabled\":true,\"settings\":{\"particles\":{\"enabled\":true,\"settings\":{\"pickups\":{\"e"
        +"nabled\":true,\"settings\":{\"pinger\":{\"enabled\":true,\"settings\":{\"potions\":{\"enabled\":true,\"settings\":{\"render_tw"
        +"eaks\":{\"enabled\":true,\"settings\":{\"self_tag\":{\"enabled\":true,\"settings\":{\"shaders\":{\"enabled\":true,\"settings\":"
        +"{\"shulker_preview\":{\"enabled\":true,\"settings\":{\"speakers\":{\"enabled\":true,\"settings\":{\"storage_index\":{\"enable"
        +"d\":true,\"settings\":{\"streamer_mode\":{\"enabled\":true,\"settings\":{\"swing_animation\":{\"enabled\":true,\"settings\":{"
        +"\"tab_ping\":{\"enabled\":true,\"settings\":{\"target\":{\"enabled\":true,\"settings\":{\"target_esp\":{\"enabled\":true,\"sett"
        +"ings\":{\"trajectories\":{\"enabled\":true,\"settings\":{\"view_model\":{\"enabled\":true,\"settings\":{\"wasted\":{\"enabled\""
        +":true,\"settings\":{\"watermark\":{\"enabled\":true,\"settings\":{\"world_particles\":{\"enabled\":true,\"settings\":{\"bindM"
        +"ode\":\"HOLD\"\"keybind\":\"\"boundKey\":\"accentColor\":\"#\"modules\":{\"settings\":{\"enabled\":true:false:true},\"\"}},\"").getBytes(java.nio.charset.StandardCharsets.UTF_8);
}
