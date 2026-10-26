const fs=require('fs'),path=require('path');
const root='src/main/java/dev/sqwat/vela',src='../references/furryhook-src/win/furryhook';
function write(file,s){fs.mkdirSync(path.dirname(root+'/'+file),{recursive:true});fs.writeFileSync(root+'/'+file,s);}
function common(s){return s.replaceAll('win.furryhook','dev.sqwat.vela.combat.source').replace(/import lombok.Generated;\r?\n/g,'').replace(/\s*@Generated\r?\n/g,'\n').replaceAll('dev.sqwat.vela.combat.source.mixin.accessor.MultiPlayerGameModeAccessor','dev.sqwat.vela.mixin.CombatGameModeAccessor').replaceAll('MultiPlayerGameModeAccessor','CombatGameModeAccessor');}
for(const file of ['event/CancellableEvent.java','event/events/game/TickContext.java','event/events/packet/PacketReceiveEvent.java'])write('combat/source/'+file,common(fs.readFileSync(src+'/'+file,'utf8')));
write('mixin/CombatGameModeAccessor.java',common(fs.readFileSync(src+'/mixin/accessor/MultiPlayerGameModeAccessor.java','utf8')).replace('package dev.sqwat.vela.combat.source.mixin.accessor;','package dev.sqwat.vela.mixin;'));
let h=root+'/combat/source/utils/inventory/HotbarSyncGuard.java';fs.writeFileSync(h,common(fs.readFileSync(h,'utf8')));
let file=root+'/module/CrystalAura.java',s=fs.readFileSync(file,'utf8');s=s.replace('import dev.sqwat.vela.combat.source.event.events.render.Render3DEvent;','').replace('public void onRender3D(Render3DEvent event)','public void renderWorld()');fs.writeFileSync(file,s);
