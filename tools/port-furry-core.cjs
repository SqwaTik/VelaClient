// One-time, explicit source migration. Run only when refreshing these imported algorithms.
const fs=require('fs'),path=require('path');
const src=path.resolve('../references/furryhook-src/win/furryhook');
const dst=path.resolve('src/main/java/dev/sqwat/vela');
function write(file,text){const out=path.join(dst,file);fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,text);}
function read(file){return fs.readFileSync(path.join(src,file),'utf8').replace(/\r\n/g,'\n');}
function common(text){return text.replaceAll('win.furryhook','dev.sqwat.vela.combat.source').replace(/import lombok.Generated;\n/g,'').replace(/\s*@Generated\n/g,'\n').replace(/import .*\.EventTarget;\n/g,'').replace(/\s*@EventTarget\n/g,'\n');}
function method(text,name,body){let re=new RegExp('^   (?:public|private|protected)[^\\n]*\\b'+name+'\\([^\\n]*\\) \\{','m'),m=re.exec(text);if(!m)throw Error('Missing method '+name);let open=text.indexOf('{',m.index),depth=1,i=open+1;for(;depth&&i<text.length;i++){if(text[i]=='{')depth++;if(text[i]=='}')depth--;}return text.slice(0,m.index)+(body??'')+text.slice(i);}
function range(text,start,end){let a=text.indexOf(start),b=text.indexOf(end,a);if(a<0||b<0)throw Error(start);return text.slice(0,a)+text.slice(b);}
const utils=['context/MinecraftContext.java','context/InventoryContext.java','context/RotationContext.java','utils/combat/AuraUtil.java','utils/combat/AuraRaycast.java','utils/combat/MultipointUtil.java','utils/combat/PredictUtils.java','utils/combat/SprintManager.java','utils/combat/TargetUtil.java','utils/combat/TargetFilter.java','utils/math/TickSimulator.java','utils/math/NoiseUtil.java','utils/inventory/InventoryUtil.java','utils/inventory/InventorySwap.java','utils/inventory/HotbarSyncGuard.java','utils/ConfigIO.java','event/events/input/PlayerInputEvent.java','event/events/input/KeyboardInputEvent.java','event/events/input/MouseInputEvent.java','event/events/game/GameTickEvent.java','event/events/game/PlayerTickEvent.java','event/Event.java'];
for(const dir of ['utils/combat/rotations','utils/combat/neuro'])for(const file of fs.readdirSync(path.join(src,dir)))if(file.endsWith('.java'))utils.push(dir+'/'+file);
for(const file of utils){let s=common(read(file));
 if(file.includes('rotations/')){s=s.replace(/import .*\.FeatureManager;\n|import .*\.AutoJumpFeature;\n/g,'');s=s.replace(/FeatureManager\.INSTANCE\.getEnabled\(AutoJumpFeature.class\) != null/g,'false');}
 if(file==='utils/inventory/InventorySwap.java')s=s.replace(/!DropAllInventoryController.blocksInventoryOperations\(\)/g,'!dev.sqwat.vela.combat.CombatRuntime.inventoryBlocked()');
 if(file==='utils/ConfigIO.java')s=s.replace('systemDriveRoot().resolve("furryhook")','FabricLoader.getInstance().getConfigDir().resolve("vela").resolve("combat")');
 write('combat/source/'+file,s);
}
const modules=[['combat/AuraFeature','AttackAura','Attack Aura','Выбор цели, ротация и условия автоматической атаки','ATTACK'],['combat/AutoWebFeature','WebTrap','WebTrap','Ставит паутину под ноги цели и над ней','ATTACK'],['combat/CrystalAuraFeature','CrystalAura','Crystal Aura','Ставит обсидиан, кристаллы и подкапывает цель','ATTACK'],['player/ElytraHelperFeature','ElytraSwap','Elytra Swap','Смена элитр, взлёт и управление ускорением','MOVEMENT'],['player/ClickPearlFeature','ClickPearl','Click Pearl','Быстрый бросок жемчуга из инвентаря','UTILITIES']];
const ids={AttackAura:'attack_aura',WebTrap:'web_trap',CrystalAura:'crystal_aura',ElytraSwap:'elytra_swap',ClickPearl:'click_pearl'};
for(const [file,clazz,label,desc,category] of modules){let s=common(read('feature/impl/'+file+'.java')),old=file.split('/')[1];
 s=s.replace(/package .*;/,'package dev.sqwat.vela.module;');
 s=s.replace(/import .*\.feature\.setting\.[^;]+;/g,'').replace(/import .*\.feature\.(Feature|FeatureCategory|FeatureManager);/g,'');
 s=s.replace('public final class '+old+' extends Feature','public final class '+old+' extends dev.sqwat.vela.combat.SourceModule');
 s=s.replace(/super\([^;]+;/,'super("'+ids[clazz]+'","'+label+'","'+desc+'",ModuleCategory.'+category+');');
 s=s.replaceAll(old,clazz);
 s=s.replace(/(\b(\w+)\s*=\s*)this.register\(/g,(_,prefix,field)=>prefix+'this.register("'+field.replace(/[A-Z]/g,c=>'_'+c.toLowerCase())+'",');
 s=s.replace(/FeatureManager\.INSTANCE\.getEnabled\(/g,'active(').replace(/FeatureManager\.INSTANCE\.getFeature\(/g,'find(');
 s=s.replace('public void onTick(GameTickEvent event)', 'public void sourceTick(GameTickEvent event)');
 s=s.replace('package dev.sqwat.vela.module;','package dev.sqwat.vela.module;\nimport dev.sqwat.vela.combat.SourceSettings.*;');
 if(clazz==='AttackAura'){
   s=s.replace(/import .*\.(AirStuckFeature|SprintFeature|ColorUtil|MsdfFont|Render2DUtil|ScaleUtil|UiFontStyle|UiFonts|Theme|SoulImpulse|StormState|ChatUtil);\n/g,'');
   s=range(s,'   public void onRender2D(','   private void resetRotationState()');
   s=s.replace(/import .*\.Render2DEvent;\n/g,'');
   s=s.replace(/import .*\.WorldLeaveEvent;\n/g,'').replace('public void onWorldLeave(WorldLeaveEvent event)','public void onWorldLeave()');
   s=method(s,'getEffectiveTargetEyePos','   private Vec3 getEffectiveTargetEyePos(LocalPlayer player, LivingEntity target, boolean attackLikely) { return player.isFallFlying() && target.isFallFlying() && this.predictElytra.getValue() ? this.realPredict(target) : target.getEyePosition(); }');
   s=s.replace(/\s*SprintFeature sprintFeature = SprintFeature.getEnabled\(\);\s*if \(sprintFeature != null && sprintFeature.keepSprint.getValue\(\)\) \{\s*player.setSprinting\(true\);\s*\}/,'');
   s=s.replace(/\s*AirStuckFeature airStuck = active\(AirStuckFeature.class\);\s*if \(airStuck != null\) \{\s*airStuck.pauseForAttack\(\);\s*\}/,'');
   s=s.replace(/\s*ElytraResolverFeature resolver = ElytraResolverFeature.getEnabled\(\);\s*if \(resolver != null && player.isFallFlying\(\) && entity instanceof LivingEntity living\) \{\s*resolver.onHit\(living\);\s*\}/,'');
   s=s.replace(/\s*this.strikeLightningTargetEsp\(player, entity\);\s*if \(this.usesSoulsTargetEsp\(\)\) \{\s*SoulImpulse.hit\(entity\);\s*\}/,'\n         dev.sqwat.vela.combat.CombatRuntime.onHit(entity);');
   s=method(s,'strikeLightningTargetEsp','');
   s=s.replace(/ElytraTargetFeature elytraFeature = ElytraTargetFeature.getEnabled\(\);\s*boolean flyboxPassed = [^;]+;/,'boolean flyboxPassed = false;');
   s=s.replace(/\s*ElytraTargetFeature etf = ElytraTargetFeature.getEnabled\(\);\s*if \(etf != null\) \{\s*lead = etf.predictPower.getValue\(\);\s*\}/,'');
   s=method(s,'attackDistance','   public double attackDistance() { return attackRange.getValue(); }');
   s=s.replace('AntiBotFeature.checkBot(entity)','entity.isSpectator()');
   s=s.replaceAll('ChatUtil.info(', 'dev.sqwat.vela.combat.CombatRuntime.info(');
   s=s.replaceAll('ChatUtil.error(', 'dev.sqwat.vela.combat.CombatRuntime.info(');
   s=s.replace('   public LivingEntity getTarget() {','   public LivingEntity target() { return getTarget(); }\n   public LivingEntity getTarget() {');
 }
 if(clazz==='CrystalAura'){
   s=s.replace(/import .*\.AutoToolFeature;\n/g,'');
   s=s.replace(/AutoToolFeature autoTool = find\(AutoToolFeature.class\);\s*if \(autoTool != null\) \{\s*autoTool.swapForBlock\(world.getBlockState\(blockPos\)\);\s*\}/,'int tool = this.findBestTool(self, world.getBlockState(blockPos));\n                              if(tool >= 0) { if(lastSlot < 0)lastSlot=self.getInventory().getSelectedSlot(); self.getInventory().setSelectedSlot(tool); managedSlot=tool; ticksToBack=3; }');
 }
 s=s.replaceAll('dev.sqwat.vela.combat.source.mixin.accessor.MultiPlayerGameModeAccessor','dev.sqwat.vela.mixin.CombatGameModeAccessor');
 s=s.replaceAll('MultiPlayerGameModeAccessor','CombatGameModeAccessor');
 s=s.replace('\n}', '\n   @Override public void onClientTick(){dev.sqwat.vela.combat.CombatRuntime.tick(this);}\n}');
 write('module/'+clazz+'.java',s);
}
// Helpers are components owned and persisted by ElytraSwap.
for(const file of ['ElytraBoostFeature','GrimElytraFeature']){
 let s=common(read('feature/impl/movement/'+file+'.java'));
 s=s.replace(/import .*\.feature\.(Feature|FeatureCategory|FeatureManager);\n/g,'').replace(/import .*\.feature\.setting\.[^;]+;/g,'');
 s=s.replace('extends Feature','extends dev.sqwat.vela.combat.SourceModule');
 s=s.replace(/super\([^;]+;/, 'super("'+(file==='ElytraBoostFeature'?'elytra_boost':'elytra_bounce')+'","'+file+'","",dev.sqwat.vela.module.ModuleCategory.MOVEMENT);');
 s=s.replace(/(\b(\w+)\s*=\s*)this.register\(/g,(_,prefix,field)=>prefix+'this.register("'+(file==='ElytraBoostFeature'?'boost_':'bounce_')+field.toLowerCase()+'",');
 s=s.replace('FeatureManager.INSTANCE.getEnabled(ElytraBoostFeature.class)','dev.sqwat.vela.module.ElytraSwap.boost()');
 s=s.replace(/(package [^;]+;)/,'$1\nimport dev.sqwat.vela.combat.SourceSettings.*;');
 write('combat/source/feature/impl/movement/'+file+'.java',s);
}
console.log('Migrated source algorithms and modules');
