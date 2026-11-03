# Vela Music HUD helper: reads Windows media sessions (SMTC) and prints one JSON object per line on stdout.
# Every app that shows in the Windows volume/media flyout is seen: browsers, Spotify, Yandex Music, players.
# Besides the song it reports how loud the playing app is right now ({"lv":0.42}, 25 times a second, from the
# Windows per-app peak meters, or with "b": a 20-band spectrum recorded from that app alone), whether its sound
# is muted, and the browser tab it plays in.
# Commands arrive on stdin, one per line: toggle, next, prev.
# Argument: the game's process id. Quits when that process is gone, so it never outlives Minecraft.
param([int]$GamePid = 0)
$ErrorActionPreference = 'SilentlyContinue'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
Add-Type -AssemblyName System.Runtime.WindowsRuntime
Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName UIAutomationClient
Add-Type -AssemblyName UIAutomationTypes

Add-Type -ErrorAction Stop -TypeDefinition @'
using System;
using System.Collections.Generic;
using System.Collections.Concurrent;
using System.Diagnostics;
using System.Globalization;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;

namespace VelaMedia {
    [ComImport, Guid("BCDE0395-E52F-467C-8E3D-C4579291692E")] class MMDeviceEnumerator {}
    [ComImport, Guid("A95664D2-9614-4F35-A746-DE8DB63617E6"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IMMDeviceEnumerator {
        [PreserveSig] int EnumAudioEndpoints(int flow, int mask, out IntPtr devices);
        [PreserveSig] int GetDefaultAudioEndpoint(int flow, int role, out IMMDevice device);
    }
    [ComImport, Guid("D666063F-1587-4E43-81F1-B948E807363F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IMMDevice {
        [PreserveSig] int Activate(ref Guid iid, int context, IntPtr parameters, [MarshalAs(UnmanagedType.IUnknown)] out object instance);
    }
    [ComImport, Guid("77AA99A0-1BD6-484F-8BC7-2C654C9A9B6F"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IAudioSessionManager2 {
        [PreserveSig] int GetAudioSessionControl(IntPtr guid, int flags, out IntPtr control);
        [PreserveSig] int GetSimpleAudioVolume(IntPtr guid, int flags, out IntPtr volume);
        [PreserveSig] int GetSessionEnumerator(out IAudioSessionEnumerator sessions);
    }
    [ComImport, Guid("E2F5BB11-0570-40CA-ACDD-3AA01277DEE8"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IAudioSessionEnumerator {
        [PreserveSig] int GetCount(out int count);
        [PreserveSig] int GetSession(int index, out IAudioSessionControl2 session);
    }
    [ComImport, Guid("bfb7ff88-7239-4fc9-8fa2-07c950be9c6d"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IAudioSessionControl2 {
        [PreserveSig] int GetState(out int state);
        [PreserveSig] int GetDisplayName(out IntPtr name);
        [PreserveSig] int SetDisplayName(IntPtr name, IntPtr context);
        [PreserveSig] int GetIconPath(out IntPtr path);
        [PreserveSig] int SetIconPath(IntPtr path, IntPtr context);
        [PreserveSig] int GetGroupingParam(out Guid group);
        [PreserveSig] int SetGroupingParam(IntPtr group, IntPtr context);
        [PreserveSig] int RegisterAudioSessionNotification(IntPtr client);
        [PreserveSig] int UnregisterAudioSessionNotification(IntPtr client);
        [PreserveSig] int GetSessionIdentifier(out IntPtr id);
        [PreserveSig] int GetSessionInstanceIdentifier(out IntPtr id);
        [PreserveSig] int GetProcessId(out uint pid);
    }
    [ComImport, Guid("C02216F6-8C67-4B5B-9D00-D008E73E0064"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IAudioMeterInformation {
        [PreserveSig] int GetPeakValue(out float peak);
    }
    [ComImport, Guid("87CE5498-68D6-44E5-9215-6DA47EF883D8"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface ISimpleAudioVolume {
        [PreserveSig] int SetMasterVolume(float level, IntPtr context);
        [PreserveSig] int GetMasterVolume(out float level);
        [PreserveSig] int SetMute(int mute, IntPtr context);
        [PreserveSig] int GetMute(out int mute);
    }
    [ComImport, Guid("1CB9AD4C-DBFA-4c32-B178-C2F568A703B2"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IAudioClient {
        [PreserveSig] int Initialize(int shareMode, uint flags, long bufferDuration, long periodicity, IntPtr format, IntPtr session);
        [PreserveSig] int GetBufferSize(out uint frames);
        [PreserveSig] int GetStreamLatency(out long latency);
        [PreserveSig] int GetCurrentPadding(out uint frames);
        [PreserveSig] int IsFormatSupported(int shareMode, IntPtr format, out IntPtr closest);
        [PreserveSig] int GetMixFormat(out IntPtr format);
        [PreserveSig] int GetDevicePeriod(out long period, out long minimum);
        [PreserveSig] int Start();
        [PreserveSig] int Stop();
        [PreserveSig] int Reset();
        [PreserveSig] int SetEventHandle(IntPtr handle);
        [PreserveSig] int GetService(ref Guid iid, [MarshalAs(UnmanagedType.IUnknown)] out object service);
    }
    [ComImport, Guid("C8ADBD64-E71E-48a0-A4DE-185C395CD317"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IAudioCaptureClient {
        [PreserveSig] int GetBuffer(out IntPtr data, out uint frames, out uint flags, out ulong devicePosition, out ulong qpcPosition);
        [PreserveSig] int ReleaseBuffer(uint frames);
        [PreserveSig] int GetNextPacketSize(out uint frames);
    }
    [ComImport, Guid("72A22D78-CDE4-431D-B8CC-843A71199B6D"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    public interface IActivateAudioInterfaceAsyncOperation {
        [PreserveSig] int GetActivateResult(out int result, [MarshalAs(UnmanagedType.IUnknown)] out object instance);
    }
    [ComImport, Guid("41D949AB-9862-444A-80F6-C261334DA5EB"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    public interface IActivateAudioInterfaceCompletionHandler {
        void ActivateCompleted(IActivateAudioInterfaceAsyncOperation operation);
    }
    [ComImport, Guid("94ea2b94-e9cc-49e0-c0ff-ee64ca8f5b90"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    public interface IAgileObject {}
    [ClassInterface(ClassInterfaceType.None), ComVisible(true)]
    public class ActivationHandler : IActivateAudioInterfaceCompletionHandler, IAgileObject {
        public readonly ManualResetEvent Done = new ManualResetEvent(false);
        public object Client; public int Result = -1;
        public void ActivateCompleted(IActivateAudioInterfaceAsyncOperation operation) {
            try { int result; object instance; operation.GetActivateResult(out result, out instance); Result = result; Client = instance; }
            finally { Done.Set(); }
        }
    }

    /**
     * Records only the shown app (process loopback, Windows 10 2004+), so the game's own sounds stay out, and turns the
     * last 1024 samples into a loudness and a 20-band spectrum. Unavailable on older systems: then the peak meters are used.
     */
    public static class Capture {
        public const int Rate = 44100, Size = 1024, Bands = 20;
        [DllImport("Mmdevapi.dll", ExactSpelling = true)]
        static extern int ActivateAudioInterfaceAsync([MarshalAs(UnmanagedType.LPWStr)] string path, ref Guid iid, IntPtr parameters,
            IActivateAudioInterfaceCompletionHandler handler, out IActivateAudioInterfaceAsyncOperation operation);
        static readonly object Gate = new object();
        static readonly float[] ring = new float[4096];
        static int head;
        static int lastData;
        public static volatile int WantedPid;
        static volatile int capturing;
        static readonly Dictionary<int, int> failed = new Dictionary<int, int>();

        public static void Start() { var t = new Thread(Run); t.IsBackground = true; t.SetApartmentState(ApartmentState.MTA); t.Start(); }
        /** True while the wanted app is being recorded (silence included). */
        public static bool Live { get { int pid = WantedPid; return pid != 0 && capturing == pid; } }

        static void Run() {
            while (true) {
                int pid = WantedPid, now = Environment.TickCount, until;
                bool skip;
                lock (failed) skip = pid == 0 || (failed.TryGetValue(pid, out until) && now - until < 0);
                if (skip) { Thread.Sleep(250); continue; }
                try { Record(pid); }
                catch { lock (failed) failed[pid] = now + 30000; }
                capturing = 0;
                Thread.Sleep(100);
            }
        }
        static void Record(int pid) {
            IntPtr blob = Marshal.AllocHGlobal(12), variant = Marshal.AllocHGlobal(24), format = Marshal.AllocHGlobal(18);
            try {
                // AUDIOCLIENT_ACTIVATION_PARAMS: process loopback of this process and its children.
                Marshal.WriteInt32(blob, 0, 1); Marshal.WriteInt32(blob, 4, pid); Marshal.WriteInt32(blob, 8, 0);
                for (int i = 0; i < 24; i++) Marshal.WriteByte(variant, i, 0);
                Marshal.WriteInt16(variant, 0, 65); // PROPVARIANT VT_BLOB
                Marshal.WriteInt32(variant, 8, 12);
                Marshal.WriteIntPtr(variant, IntPtr.Size == 8 ? 16 : 12, blob);
                var handler = new ActivationHandler();
                Guid iid = typeof(IAudioClient).GUID;
                IActivateAudioInterfaceAsyncOperation operation;
                int hr = ActivateAudioInterfaceAsync("VAD\\Process_Loopback", ref iid, variant, handler, out operation);
                if (hr != 0 || !handler.Done.WaitOne(3000) || handler.Result != 0 || handler.Client == null) throw new Exception("activate");
                var client = (IAudioClient)handler.Client;
                // 16-bit stereo PCM; Windows converts whatever the app plays.
                Marshal.WriteInt16(format, 0, 1); Marshal.WriteInt16(format, 2, 2); Marshal.WriteInt32(format, 4, Rate);
                Marshal.WriteInt32(format, 8, Rate * 4); Marshal.WriteInt16(format, 12, 4); Marshal.WriteInt16(format, 14, 16); Marshal.WriteInt16(format, 16, 0);
                uint flags = 0x00020000u | 0x00040000u | 0x80000000u | 0x08000000u; // loopback, event, convert, good resampler
                if (client.Initialize(0, flags, 200000, 0, format, IntPtr.Zero) != 0) throw new Exception("init");
                using (var ready = new AutoResetEvent(false)) {
                    if (client.SetEventHandle(ready.SafeWaitHandle.DangerousGetHandle()) != 0) throw new Exception("event");
                    object service; iid = typeof(IAudioCaptureClient).GUID;
                    if (client.GetService(ref iid, out service) != 0) throw new Exception("service");
                    var capture = (IAudioCaptureClient)service;
                    if (client.Start() != 0) throw new Exception("start");
                    capturing = pid;
                    var samples = new short[16384];
                    try {
                        while (WantedPid == pid) {
                            ready.WaitOne(100);
                            uint packet;
                            while (capture.GetNextPacketSize(out packet) == 0 && packet > 0) {
                                IntPtr data; uint frames, bufferFlags; ulong devicePosition, qpcPosition;
                                if (capture.GetBuffer(out data, out frames, out bufferFlags, out devicePosition, out qpcPosition) != 0) break;
                                int count = (int)Math.Min(frames, (uint)(samples.Length / 2));
                                bool silent = (bufferFlags & 2) != 0;
                                if (!silent && count > 0) Marshal.Copy(data, samples, 0, count * 2);
                                lock (Gate) {
                                    for (int i = 0; i < count; i++) {
                                        ring[head] = silent ? 0 : (samples[i * 2] + samples[i * 2 + 1]) / 65536f;
                                        head = (head + 1) & (ring.Length - 1);
                                    }
                                    lastData = Environment.TickCount;
                                }
                                capture.ReleaseBuffer(frames);
                            }
                        }
                    } finally { client.Stop(); }
                }
            } finally { Marshal.FreeHGlobal(blob); Marshal.FreeHGlobal(variant); Marshal.FreeHGlobal(format); }
        }

        static readonly float[] re = new float[Size], im = new float[Size], window = MakeWindow();
        static float[] MakeWindow() { var w = new float[Size]; for (int i = 0; i < Size; i++) w[i] = (float)(.5 - .5 * Math.Cos(2 * Math.PI * i / (Size - 1))); return w; }
        /** Loudness (0..1) and spectrum (0..1 per band) of the last {@link #Size} samples; all zero after 200 ms without data. */
        public static bool Analyse(float[] bands, out float level) {
            level = 0;
            double sum = 0;
            lock (Gate) {
                if (Environment.TickCount - lastData > 200) { for (int i = 0; i < bands.Length; i++) bands[i] = 0; return true; }
                for (int i = 0; i < Size; i++) {
                    float v = ring[(head - Size + i) & (ring.Length - 1)];
                    re[i] = v * window[i]; im[i] = 0; sum += v * v;
                }
            }
            double rms = Math.Sqrt(sum / Size);
            level = (float)Math.Max(0, Math.Min(1, (20 * Math.Log10(rms + 1e-9) + 48) / 44));
            Fft();
            double low = 90, high = 14000, binHz = (double)Rate / Size;
            for (int b = 0; b < bands.Length; b++) {
                double from = low * Math.Pow(high / low, (double)b / bands.Length), to = low * Math.Pow(high / low, (double)(b + 1) / bands.Length);
                int i0 = Math.Max(1, (int)(from / binHz)), i1 = Math.Max(i0 + 1, (int)(to / binHz));
                double power = 0;
                for (int i = i0; i < i1 && i < Size / 2; i++) power += re[i] * re[i] + im[i] * im[i];
                double magnitude = Math.Sqrt(power) / (Size / 4.0);
                // Music has less energy up high: tilt those bands up so the right side moves too.
                double db = 20 * Math.Log10(magnitude + 1e-9) + 18.0 * b / bands.Length;
                bands[b] = (float)Math.Max(0, Math.Min(1, (db + 62) / 56));
            }
            return true;
        }
        static void Fft() {
            int n = Size;
            for (int i = 1, j = 0; i < n; i++) {
                int bit = n >> 1;
                for (; (j & bit) != 0; bit >>= 1) j ^= bit;
                j ^= bit;
                if (i < j) { float t = re[i]; re[i] = re[j]; re[j] = t; t = im[i]; im[i] = im[j]; im[j] = t; }
            }
            for (int length = 2; length <= n; length <<= 1) {
                double angle = -2 * Math.PI / length;
                float wr = (float)Math.Cos(angle), wi = (float)Math.Sin(angle);
                for (int i = 0; i < n; i += length) {
                    float cr = 1, ci = 0;
                    for (int k = 0; k < length / 2; k++) {
                        int a = i + k, b = a + length / 2;
                        float xr = re[b] * cr - im[b] * ci, xi = re[b] * ci + im[b] * cr;
                        re[b] = re[a] - xr; im[b] = im[a] - xi; re[a] += xr; im[a] += xi;
                        float next = cr * wr - ci * wi; ci = cr * wi + ci * wr; cr = next;
                    }
                }
            }
        }
    }

    [ComImport, Guid("5CDF2C82-841E-4546-9722-0CF74078229A"), InterfaceType(ComInterfaceType.InterfaceIsIUnknown)]
    interface IAudioEndpointVolume {
        [PreserveSig] int RegisterControlChangeNotify(IntPtr notify);
        [PreserveSig] int UnregisterControlChangeNotify(IntPtr notify);
        [PreserveSig] int GetChannelCount(out int count);
        [PreserveSig] int SetMasterVolumeLevel(float level, IntPtr context);
        [PreserveSig] int SetMasterVolumeLevelScalar(float level, IntPtr context);
        [PreserveSig] int GetMasterVolumeLevel(out float level);
        [PreserveSig] int GetMasterVolumeLevelScalar(out float level);
        [PreserveSig] int SetChannelVolumeLevel(uint channel, float level, IntPtr context);
        [PreserveSig] int SetChannelVolumeLevelScalar(uint channel, float level, IntPtr context);
        [PreserveSig] int GetChannelVolumeLevel(uint channel, out float level);
        [PreserveSig] int GetChannelVolumeLevelScalar(uint channel, out float level);
        [PreserveSig] int SetMute(int mute, IntPtr context);
        [PreserveSig] int GetMute(out int mute);
    }

    /** Per-app sound levels from the default output device, sampled on an own MTA thread, plus the stdin reader. */
    public static class Audio {
        class Session { public uint Pid; public string Name; public IAudioMeterInformation Meter; public ISimpleAudioVolume Volume; public float Recent; }
        static readonly object Gate = new object();
        static List<Session> sessions = new List<Session>();
        static Dictionary<string, float> loudness = new Dictionary<string, float>();
        static readonly Dictionary<uint, string> names = new Dictionary<uint, string>();
        static readonly ConcurrentQueue<string> commands = new ConcurrentQueue<string>();
        static int gamePid;
        /** Process names (lower case, no ".exe") of the app being shown; its level is printed while Active. */
        public static volatile string[] Target = new string[0];
        public static volatile bool Active;
        /** The shown app's sound is off: muted in the mixer, its volume at zero, or the whole output muted. */
        public static volatile bool Muted;
        static volatile bool systemMuted;
        static readonly float[] bands = new float[Capture.Bands];

        public static void Start(int game) {
            gamePid = game;
            Capture.Start();
            var sampler = new Thread(Run); sampler.IsBackground = true; sampler.SetApartmentState(ApartmentState.MTA); sampler.Start();
            var reader = new Thread(ReadCommands); reader.IsBackground = true; reader.Start();
        }
        public static string NextCommand() { string command; return commands.TryDequeue(out command) ? command : null; }
        /** Recent peak of an app (0..1, decaying over ~1.5 s), or -1 when it has no sound session at all. */
        public static float Loudness(string name) {
            lock (Gate) { float value; return loudness.TryGetValue(name.ToLowerInvariant(), out value) ? value : -1f; }
        }

        static void ReadCommands() {
            try {
                string line;
                while ((line = Console.In.ReadLine()) != null) { line = line.Trim(); if (line.Length > 0) commands.Enqueue(line); }
            } catch { }
        }
        static string NameOf(uint pid) {
            string name;
            if (names.TryGetValue(pid, out name)) return name;
            try { name = Process.GetProcessById((int)pid).ProcessName.ToLowerInvariant(); } catch { name = null; }
            if (names.Count > 512) names.Clear();
            names[pid] = name;
            return name;
        }
        static void Run() {
            IMMDeviceEnumerator enumerator = null; IAudioSessionManager2 manager = null; IAudioEndpointVolume endpoint = null;
            for (int tick = 0; ; tick++) {
                try {
                    if (enumerator == null) enumerator = (IMMDeviceEnumerator)(object)new MMDeviceEnumerator();
                    if (tick % 25 == 0) {
                        // Once a second: the default device may have changed (headphones), and apps come and go.
                        IMMDevice device;
                        if (enumerator.GetDefaultAudioEndpoint(0, 1, out device) == 0 && device != null) {
                            object instance; Guid iid = typeof(IAudioSessionManager2).GUID;
                            if (device.Activate(ref iid, 23, IntPtr.Zero, out instance) == 0) manager = (IAudioSessionManager2)instance;
                            iid = typeof(IAudioEndpointVolume).GUID;
                            if (device.Activate(ref iid, 23, IntPtr.Zero, out instance) == 0) endpoint = (IAudioEndpointVolume)instance;
                        }
                        Refresh(manager);
                        if (endpoint != null) {
                            int mute; float volume;
                            bool muted = endpoint.GetMute(out mute) == 0 && mute != 0;
                            if (endpoint.GetMasterVolumeLevelScalar(out volume) == 0 && volume < .005f) muted = true;
                            systemMuted = muted;
                        }
                    }
                    Sample();
                } catch {
                    enumerator = null; manager = null; endpoint = null;
                    lock (Gate) sessions = new List<Session>();
                }
                Thread.Sleep(40);
            }
        }
        static void Refresh(IAudioSessionManager2 manager) {
            var next = new List<Session>();
            IAudioSessionEnumerator list;
            if (manager != null && manager.GetSessionEnumerator(out list) == 0 && list != null) {
                int count; list.GetCount(out count);
                List<Session> previous; lock (Gate) previous = sessions;
                for (int i = 0; i < count; i++) {
                    IAudioSessionControl2 control;
                    if (list.GetSession(i, out control) != 0 || control == null) continue;
                    uint pid; control.GetProcessId(out pid);
                    if (pid == 0 || pid == gamePid) continue;
                    var session = new Session { Pid = pid, Name = NameOf(pid), Meter = control as IAudioMeterInformation, Volume = control as ISimpleAudioVolume };
                    if (session.Name == null || session.Meter == null) continue;
                    foreach (var old in previous) if (old.Pid == pid) session.Recent = Math.Max(session.Recent, old.Recent);
                    next.Add(session);
                }
            }
            lock (Gate) sessions = next;
        }
        static void Sample() {
            List<Session> list; lock (Gate) list = sessions;
            string[] target = Target;
            float level = 0, loudest = 0, best = -1; bool found = false, allMuted = true; uint bestPid = 0;
            var loud = new Dictionary<string, float>();
            foreach (var session in list) {
                float peak = 0; bool muted = false;
                try { session.Meter.GetPeakValue(out peak); } catch { }
                if (session.Volume != null) {
                    try { int mute; float volume; session.Volume.GetMute(out mute); session.Volume.GetMasterVolume(out volume); muted = mute != 0 || volume < .005f; } catch { }
                }
                if (muted) peak = 0;
                session.Recent = Math.Max(peak, session.Recent * .96f);
                float known; loud.TryGetValue(session.Name, out known);
                loud[session.Name] = Math.Max(known, session.Recent);
                if (Array.IndexOf(target, session.Name) >= 0) {
                    found = true; level = Math.Max(level, peak); if (!muted) allMuted = false;
                    if (session.Recent > best) { best = session.Recent; bestPid = session.Pid; }
                }
                loudest = Math.Max(loudest, peak);
            }
            lock (Gate) loudness = loud;
            Muted = systemMuted || (found && allMuted);
            // Record the process that actually renders the sound (a browser plays from its audio child process).
            Capture.WantedPid = Active && found ? (int)bestPid : 0;
            if (!Active) return;
            var text = new StringBuilder("{\"lv\":");
            float recorded;
            if (Capture.Live && Capture.Analyse(bands, out recorded)) {
                if (Muted) { recorded = 0; for (int i = 0; i < bands.Length; i++) bands[i] = 0; }
                text.Append(recorded.ToString("0.###", CultureInfo.InvariantCulture)).Append(",\"b\":[");
                for (int i = 0; i < bands.Length; i++) { if (i > 0) text.Append(','); text.Append(bands[i].ToString("0.###", CultureInfo.InvariantCulture)); }
                text.Append(']');
            } else {
                // No recording (older Windows, unmatched app): the peak meter, or the loudest other sound.
                if (!found) level = loudest;
                if (Muted) level = 0;
                text.Append(level.ToString("0.###", CultureInfo.InvariantCulture));
            }
            Console.Out.WriteLine(text.Append('}').ToString());
        }

        delegate bool EnumWindowsProc(IntPtr window, IntPtr data);
        [DllImport("user32.dll")] static extern bool EnumWindows(EnumWindowsProc callback, IntPtr data);
        [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr window, out uint pid);
        [DllImport("user32.dll")] static extern bool IsWindowVisible(IntPtr window);
        [DllImport("user32.dll", CharSet = CharSet.Unicode)] static extern int GetWindowText(IntPtr window, StringBuilder text, int max);
        /** Titles of the visible top-level windows of these processes: a browser window is named after its open tab. */
        public static string[] WindowTitles(int[] pids) {
            var titles = new List<string>();
            var wanted = new HashSet<uint>(); foreach (int pid in pids) wanted.Add((uint)pid);
            EnumWindows((window, data) => {
                uint pid; GetWindowThreadProcessId(window, out pid);
                if (wanted.Contains(pid) && IsWindowVisible(window)) {
                    var text = new StringBuilder(512);
                    if (GetWindowText(window, text, text.Capacity) > 0) titles.Add(text.ToString());
                }
                return true;
            }, IntPtr.Zero);
            return titles.ToArray();
        }
    }
}
'@

$asTask = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object {
    $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]
function Await($op, [Type]$type) {
    $task = $asTask.MakeGenericMethod($type).Invoke($null, @($op))
    if (-not $task.Wait(3000)) { return $null }
    $task.Result
}
function Emit($object) { [Console]::Out.WriteLine(($object | ConvertTo-Json -Compress)) }
$null = [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType = WindowsRuntime]
$null = [Windows.Storage.Streams.IRandomAccessStreamWithContentType, Windows.Storage.Streams, ContentType = WindowsRuntime]
$null = [Windows.ApplicationModel.AppInfo, Windows.ApplicationModel, ContentType = WindowsRuntime]
$manager = Await ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])
if ($null -eq $manager) { [Console]::Out.WriteLine('{"error":"no_manager"}'); exit 1 }
[VelaMedia.Audio]::Start($GamePid)

function ToPng($bytes, [int]$max) {
    try {
        $ms = New-Object System.IO.MemoryStream(, $bytes)
        $img = [System.Drawing.Image]::FromStream($ms)
        $size = [Math]::Min($max, [Math]::Max($img.Width, $img.Height))
        $bmp = New-Object System.Drawing.Bitmap($size, $size)
        $gfx = [System.Drawing.Graphics]::FromImage($bmp)
        $gfx.InterpolationMode = 'HighQualityBicubic'
        # Centre-crop to a square.
        $side = [Math]::Min($img.Width, $img.Height)
        $src = New-Object System.Drawing.Rectangle([int](($img.Width - $side) / 2), [int](($img.Height - $side) / 2), $side, $side)
        $gfx.DrawImage($img, (New-Object System.Drawing.Rectangle(0, 0, $size, $size)), $src, 'Pixel')
        $out = New-Object System.IO.MemoryStream
        $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
        $gfx.Dispose(); $bmp.Dispose(); $img.Dispose()
        [Convert]::ToBase64String($out.ToArray())
    } catch { $null }
}
function ReadStream($ref, [int]$max = 256) {
    if ($null -eq $ref) { return $null }
    $stream = Await ($ref.OpenReadAsync()) ([Windows.Storage.Streams.IRandomAccessStreamWithContentType])
    if ($null -eq $stream) { return $null }
    try {
        $net = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($stream)
        $ms = New-Object System.IO.MemoryStream
        $net.CopyTo($ms)
        if ($ms.Length -le 0 -or $ms.Length -gt 8MB) { return $null }
        # Normalize to PNG: covers come as JPEG/PNG/BMP, Minecraft textures only read PNG.
        ToPng $ms.ToArray() $max
    } catch { $null }
}
function FileIcon($path) {
    try {
        $ico = [System.Drawing.Icon]::ExtractAssociatedIcon($path)
        $bmp = $ico.ToBitmap(); $out = New-Object System.IO.MemoryStream
        $bmp.Save($out, [System.Drawing.Imaging.ImageFormat]::Png)
        [Convert]::ToBase64String($out.ToArray())
    } catch { $null }
}

# Start menu shortcuts by their AppUserModelID: apps like Yandex Music register an id ("ru.yandex.desktop.music")
# that is neither a store package nor an exe name; the shortcut tells the real name and program.
$shortcuts = $null
function Shortcut($aumid) {
    if ($null -eq $script:shortcuts) {
        $script:shortcuts = @{}
        try {
            $shell = New-Object -ComObject Shell.Application
            $wsh = New-Object -ComObject WScript.Shell
            foreach ($root in @("$env:APPDATA\Microsoft\Windows\Start Menu\Programs", "$env:ProgramData\Microsoft\Windows\Start Menu\Programs")) {
                Get-ChildItem -LiteralPath $root -Filter *.lnk -Recurse -ErrorAction SilentlyContinue | ForEach-Object {
                    $item = $shell.Namespace($_.DirectoryName).ParseName($_.Name)
                    $id = $item.ExtendedProperty('System.AppUserModel.ID')
                    if ($id -and -not $script:shortcuts.ContainsKey("$id")) {
                        $script:shortcuts["$id"] = @{ name = $_.BaseName; target = $wsh.CreateShortcut($_.FullName).TargetPath }
                    }
                }
            }
        } catch {}
    }
    $script:shortcuts[$aumid]
}

$browsers = @('chrome', 'msedge', 'brave', 'opera', 'opera_gx', 'firefox', 'browser', 'vivaldi', 'arc', 'zen', 'librewolf', 'floorp', 'thorium', 'chromium', 'yandex', 'waterfox', 'iridium', 'atom')
$apps = @{}
function AppInfo($aumid) {
    if ($apps.ContainsKey($aumid)) { return $apps[$aumid] }
    $name = $null; $icon = $null; $procs = @()
    try {
        $info = [Windows.ApplicationModel.AppInfo]::GetFromAppUserModelId($aumid)
        if ($null -ne $info) {
            $name = $info.DisplayInfo.DisplayName
            $icon = ReadStream ($info.DisplayInfo.GetLogo((New-Object Windows.Foundation.Size(64, 64)))) 64
        }
    } catch {}
    # Plain desktop apps report their exe name ("brave.exe"): take name and icon from the running process.
    $exe = $aumid -replace '^.*[\\/]', '' -replace '!.*$', ''
    $base = $exe -replace '\.exe$', ''
    $proc = Get-Process -Name $base -ErrorAction SilentlyContinue | Where-Object { $_.Path } | Select-Object -First 1
    if ($null -eq $proc -and $aumid.Contains('!')) {
        # A store app: its processes run from the package folder.
        $package = ($aumid -split '!')[0] -replace '_[a-z0-9]{13}$', ''
        $proc = Get-Process | Where-Object { $_.Path -and $_.Path -like "*\WindowsApps\$($package)_*" } | Select-Object -First 1
    }
    if ($null -eq $proc) {
        $link = Shortcut $aumid
        if ($null -ne $link) {
            if (-not $name) { $name = $link.name }
            if ($link.target) {
                if (-not $icon) { $icon = FileIcon $link.target }
                $leaf = [System.IO.Path]::GetFileNameWithoutExtension($link.target)
                if ($leaf) { $procs += $leaf.ToLowerInvariant() }
            }
        }
    }
    if ($null -ne $proc) {
        $procs += $proc.ProcessName.ToLowerInvariant()
        if (-not $name) { $name = $proc.MainModule.FileVersionInfo.FileDescription }
        if (-not $name) { $name = $proc.MainModule.FileVersionInfo.ProductName }
        if (-not $icon) { $icon = FileIcon $proc.Path }
    }
    if ($procs.Count -eq 0 -and $base) { $procs += $base.ToLowerInvariant() }
    $isBrowser = $false
    foreach ($p in $procs) { if ($browsers -contains $p) { $isBrowser = $true } }
    if (-not $name) { $name = $base }
    $entry = @{ name = $name; icon = $icon; procs = [string[]]$procs; browser = $isBrowser }
    $apps[$aumid] = $entry
    $entry
}

# The browser tab that plays: a window titled after it (cheap), else the tab strip through UI Automation.
function TabName($info, [string]$title, [string]$artist) {
    $pids = @(Get-Process -Name $info.procs -ErrorAction SilentlyContinue | ForEach-Object { $_.Id })
    if ($pids.Count -eq 0) { return $null }
    $t = $title.ToLowerInvariant(); $a = $artist.ToLowerInvariant()
    foreach ($name in [VelaMedia.Audio]::WindowTitles([int[]]$pids)) {
        if ($t.Length -gt 0 -and $name.ToLowerInvariant().Contains($t)) { return $name }
    }
    $fallback = $null
    try {
        $tabCondition = New-Object System.Windows.Automation.PropertyCondition([System.Windows.Automation.AutomationElement]::ControlTypeProperty, [System.Windows.Automation.ControlType]::TabItem)
        $windows = [System.Windows.Automation.AutomationElement]::RootElement.FindAll([System.Windows.Automation.TreeScope]::Children, [System.Windows.Automation.Condition]::TrueCondition)
        foreach ($window in $windows) {
            if ($pids -notcontains $window.Current.ProcessId) { continue }
            foreach ($tab in $window.FindAll([System.Windows.Automation.TreeScope]::Descendants, $tabCondition)) {
                $name = "$($tab.Current.Name)"; $lower = $name.ToLowerInvariant()
                if ($t.Length -gt 0 -and $lower.Contains($t)) { return $name }
                if ($null -eq $fallback -and $a.Length -ge 3 -and $lower.Contains($a)) { $fallback = $name }
            }
        }
    } catch {}
    $fallback
}

function Score($s) {
    $info = AppInfo $s.SourceAppUserModelId
    $status = "$($s.GetPlaybackInfo().PlaybackStatus)"
    $score = 0
    if ($status -eq 'Playing') { $score += 100 } elseif ($status -eq 'Paused') { $score += 10 }
    # Music apps win over browsers; among the rest, the one you can hear wins.
    if (-not $info.browser) { $score += 50 }
    $loud = -1
    foreach ($p in $info.procs) { $loud = [Math]::Max($loud, [VelaMedia.Audio]::Loudness($p)) }
    if ($loud -gt .01) { $score += 30 }
    if ($s.SourceAppUserModelId -eq $script:chosen) {
        $score += 8
        # Switching songs, a player is paused or stopped for a moment; another tab that plays meanwhile (a stream in
        # the background) must not take over for that moment. The chosen player counts as playing for a few seconds.
        if ($status -ne 'Playing' -and [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() - $script:chosenPlayedAt -lt $script:SwitchGraceMs) { $score += 130 }
    }
    $score
}

$chosen = $null; $session = $null; $chosenPlayedAt = 0; $SwitchGraceMs = 4000
$lastKey = $null; $lastApp = $null; $songStart = 0
$coverSent = $null; $coverStep = 0
$coverPlan = @(0, 300, 700, 1200, 1800, 2600, 3600, 5000, 6500, 8500, 11000, 14000, 18000, 23000, 30000)
$site = ''; $siteTries = 0; $siteAt = 0

function Poll {
    $now = [long][DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
    $best = $null; $bestScore = -1
    foreach ($s in @($manager.GetSessions())) {
        $score = Score $s
        if ($score -gt $bestScore) { $best = $s; $bestScore = $score }
    }
    $script:session = $best
    if ($null -eq $best) {
        [VelaMedia.Audio]::Active = $false
        if ($script:lastKey -ne '') { [Console]::Out.WriteLine('{"state":"none"}'); $script:lastKey = ''; $script:chosen = $null }
        return
    }
    $aumid = $best.SourceAppUserModelId
    if ($aumid -ne $script:chosen) { $script:chosenPlayedAt = 0 }
    $script:chosen = $aumid
    if ("$($best.GetPlaybackInfo().PlaybackStatus)" -eq 'Playing') { $script:chosenPlayedAt = $now }
    $info = AppInfo $aumid
    $props = Await ($best.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])
    $timeline = $best.GetTimelineProperties()
    $playback = $best.GetPlaybackInfo()
    [VelaMedia.Audio]::Target = $info.procs
    [VelaMedia.Audio]::Active = "$($playback.PlaybackStatus)" -eq 'Playing'
    $controls = $playback.Controls
    $out = [ordered]@{
        state    = "$($playback.PlaybackStatus)"
        app      = $aumid
        title    = "$($props.Title)"
        artist   = "$($props.Artist)"
        album    = "$($props.AlbumTitle)"
        position = [long]$timeline.Position.TotalMilliseconds
        duration = [long]($timeline.EndTime - $timeline.StartTime).TotalMilliseconds
        updated  = [long]$timeline.LastUpdatedTime.ToUnixTimeMilliseconds()
        now      = $now
        muted    = [VelaMedia.Audio]::Muted
        browser  = [bool]$info.browser
        canPrev  = [bool]$controls.IsPreviousEnabled
        canNext  = [bool]$controls.IsNextEnabled
        canToggle = [bool]$controls.IsPlayPauseToggleEnabled
    }
    $key = "$aumid|$($props.Title)|$($props.Artist)"
    if ($key -ne $script:lastKey) {
        $script:lastKey = $key; $script:songStart = $now; $script:coverStep = 0; $script:coverSent = $null
        $script:site = ''; $script:siteTries = 0; $script:siteAt = 0
        $out.cover = $null
    }
    # Players often publish the cover a moment after the title (browsers can take seconds to fetch a video's
    # thumbnail, and some first repeat the old one): look again all through the first half minute, and send it
    # whenever it changed.
    $plan = $script:coverPlan
    if ($script:coverStep -lt $plan.Count -and $now - $script:songStart -ge $plan[$script:coverStep]) {
        $script:coverStep++
        $cover = ReadStream $props.Thumbnail
        if ($cover -and $cover -ne $script:coverSent) { $out.cover = $cover; $script:coverSent = $cover }
    }
    if ($info.browser -and $script:site -eq '' -and $script:siteTries -lt 4 -and $now -ge $script:siteAt) {
        $script:siteTries++; $script:siteAt = $now + 3000
        $tab = TabName $info "$($props.Title)" "$($props.Artist)"
        if ($tab) { $script:site = $tab }
    }
    $out.site = $script:site
    if ($aumid -ne $script:lastApp) {
        $out.appName = $info.name; $out.appIcon = $info.icon
        $script:lastApp = $aumid
    }
    Emit $out
}

function Command([string]$command) {
    $s = $script:session
    if ($null -eq $s) { return }
    switch ($command) {
        'toggle' { $null = Await ($s.TryTogglePlayPauseAsync()) ([bool]) }
        'next' { $null = Await ($s.TrySkipNextAsync()) ([bool]) }
        'prev' { $null = Await ($s.TrySkipPreviousAsync()) ([bool]) }
        default {
            # seek:<ms>; the API counts in 100 ns ticks.
            if ($command -match '^seek:(\d+)$') { $null = Await ($s.TryChangePlaybackPositionAsync([long]$Matches[1] * 10000)) ([bool]) }
        }
    }
}

for ($tick = 0; ; $tick++) {
    if ($tick % 5 -eq 0) {
        if ($GamePid -gt 0 -and $null -eq (Get-Process -Id $GamePid -ErrorAction SilentlyContinue)) { exit 0 }
        Poll
    }
    $command = [VelaMedia.Audio]::NextCommand()
    if ($null -ne $command) {
        while ($null -ne $command) { Command $command; $command = [VelaMedia.Audio]::NextCommand() }
        # Show the new state right away instead of on the next regular poll.
        Start-Sleep -Milliseconds 150; Poll; $tick = 0
    }
    Start-Sleep -Milliseconds 100
}
