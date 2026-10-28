package dev.sqwat.vela;
import dev.sqwat.vela.core.*;
import dev.sqwat.vela.module.ModuleRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;
class ConfigShareTest {
 @TempDir Path root;
 @Test void duplicateLegacyProfileCannotReappearAfterArchive()throws Exception{
  java.nio.file.Files.createDirectories(root.resolve("profiles"));
  java.nio.file.Files.writeString(root.resolve("profiles/old.json"),"{\"accentColor\":\"#123456\"}");
  java.nio.file.Files.writeString(root.resolve("profiles/old.vela"),"{\"accentColor\":\"#abcdef\"}");
  try(var service=new ConfigService(root,new ModuleRegistry())){
   assertTrue(java.nio.file.Files.exists(root.resolve("profiles/legacy-backup/old.json")));
   service.selectProfile("old");assertEquals("#abcdef",service.current().accentColor);
   service.archiveProfile("old");
  }
  try(var service=new ConfigService(root,new ModuleRegistry())){assertFalse(service.profiles().contains("old"));}
 }
 @Test void readsBothLegacyVersionsAndCompressesMore()throws Exception{
  var config=new ClientConfig();config.accentColor="#123456";
  for(int i=0;i<30;i++)config.modules.put("module-"+i,new ClientConfig.ModuleState());
  byte[] json=new com.google.gson.Gson().toJson(config).getBytes(java.nio.charset.StandardCharsets.UTF_8);
  String v1="VELA1:"+java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(json);
  var bytes=new java.io.ByteArrayOutputStream();try(var gzip=new java.util.zip.GZIPOutputStream(bytes)){gzip.write(json);}
  String v2="VELA2:"+java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes.toByteArray());
  assertEquals(config.accentColor,ConfigShare.decode(v1).accentColor);assertEquals(config.accentColor,ConfigShare.decode(v2).accentColor);
  assertTrue(ConfigShare.encode(config).length()<v2.length());
 }
 @Test void legacyProfileMigratesWithoutLosingSettings()throws Exception{
  java.nio.file.Files.createDirectories(root.resolve("profiles"));
  java.nio.file.Files.writeString(root.resolve("profiles/default.json"),"{\"schema\":1,\"accentColor\":\"#123456\"}");
  try(var service=new ConfigService(root,new ModuleRegistry())){
   assertEquals("#123456",service.current().accentColor);
   assertTrue(java.nio.file.Files.exists(service.profilePath("default")));
   assertFalse(java.nio.file.Files.exists(root.resolve("profiles/default.json")));
   service.renameProfile("default","renamed");assertTrue(java.nio.file.Files.exists(service.profilePath("renamed")));
  }
 }
 @Test void compactPayloadRestoresDefaultsAndCustomValues() {
  var config=new ClientConfig();config.accentColor="#123456";
  var state=new ClientConfig.ModuleState();state.enabled=true;state.settings.put("amount",new com.google.gson.JsonPrimitive(42));config.modules.put("example",state);
  String key=ConfigShare.encode(config);var restored=ConfigShare.decode(key);
  assertTrue(key.startsWith("VELA4:"));assertEquals(config.accentColor,restored.accentColor);
  assertTrue(restored.animations);assertEquals(-1,restored.modules.get("example").boundKey);
  assertEquals(42,restored.modules.get("example").settings.get("amount").getAsInt());
  assertTrue(ConfigShare.encode(new ClientConfig()).length()<40);
 }
 @Test void transfersBetweenIndependentPlayers()throws Exception{
  try(var a=new ConfigService(root.resolve("a"),new ModuleRegistry());var b=new ConfigService(root.resolve("b"),new ModuleRegistry())){
   a.current().accentColor="#123456";String key=a.shareProfile("default");
   assertEquals("imported",b.importProfile(key));assertEquals("#123456",b.current().accentColor);
   assertEquals("imported-2",b.importProfile(key));
  }
 }
 @Test void rejectsInvalidAndOversizedKeys(){assertThrows(IllegalArgumentException.class,()->ConfigShare.decode("bad"));assertThrows(IllegalArgumentException.class,()->ConfigShare.decode("VELA1:"+"a".repeat(90000)));assertThrows(IllegalArgumentException.class,()->ConfigShare.decode("VELA1:bnVsbA"));}
}
