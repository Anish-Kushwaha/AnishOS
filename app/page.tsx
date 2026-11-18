"use client";
import { FormEvent, useEffect, useMemo, useState } from "react";
import type { AuthChangeEvent, Session, User } from "@supabase/supabase-js";
import { createClient } from "@/lib/supabase/client";
type Device = { id: string; device_name: string; device_model: string | null; status: "pending" | "paired" | "revoked"; created_at: string };
export default function Home() {
  const supabase = useMemo(() => createClient(), []);
  const [user,setUser] = useState<User|null>(null);
  const [loading,setLoading] = useState(true);
  const [busy,setBusy] = useState(false);
  const [mode,setMode] = useState<"signin"|"signup">("signin");
  const [email,setEmail] = useState("");
  const [password,setPassword] = useState("");
  const [displayName,setDisplayName] = useState("");
  const [deviceName,setDeviceName] = useState("My Galaxy Watch4");
  const [devices,setDevices] = useState<Device[]>([]);
  const [message,setMessage] = useState("");
  const [error,setError] = useState("");
  useEffect(() => {
    let active=true;
    supabase.auth.getSession().then(({data}: {data:{session:Session|null}})=>{if(active){setUser(data.session?.user??null);setLoading(false);}});
    const {data:listener}=supabase.auth.onAuthStateChange((_event:AuthChangeEvent,session:Session|null)=>setUser(session?.user??null));
    return ()=>{active=false;listener.subscription.unsubscribe();};
  },[supabase]);
  useEffect(()=>{
    if(!user){setDevices([]);return;}
    let active=true;
    const load=async()=>{
      const {data,error:queryError}=await supabase.from("paired_devices").select("id,device_name,device_model,status,created_at").order("created_at",{ascending:false});
      if(!active)return;
      if(queryError)setError(queryError.message);else setDevices((data??[]) as Device[]);
      const {data:profile}=await supabase.from("profiles").select("id").eq("id",user.id).maybeSingle();
      if(!profile)await supabase.from("profiles").insert({id:user.id,display_name:user.user_metadata?.display_name??user.email?.split("@")[0]??"AnishOS user"});
    };
    load();return()=>{active=false;};
  },[user,supabase]);
  async function authenticate(event:FormEvent<HTMLFormElement>){
    event.preventDefault();setBusy(true);setError("");setMessage("");
    try{
      if(mode==="signup"){
        const {data,error:authError}=await supabase.auth.signUp({email,password,options:{data:{display_name:displayName.trim()}}});
        if(authError)throw authError;
        if(!data.session)setMessage("Account created. Check your email to confirm your address, then sign in.");
        else setMessage("Your AnishOS account is ready.");
      }else{
        const {error:authError}=await supabase.auth.signInWithPassword({email,password});
        if(authError)throw authError;
      }
    }catch(e){setError(e instanceof Error?e.message:"Authentication failed.");}
    finally{setBusy(false);}
  }
  async function registerWatch(event:FormEvent<HTMLFormElement>){
    event.preventDefault();if(!user||!deviceName.trim())return;
    setBusy(true);setError("");setMessage("");
    const {data,error:insertError}=await supabase.from("paired_devices").insert({user_id:user.id,device_name:deviceName.trim(),device_model:"Samsung Galaxy Watch4",device_type:"wear_os",status:"pending"}).select("id,device_name,device_model,status,created_at").single();
    if(insertError)setError(insertError.message);
    else{setDevices(current=>[data as Device,...current]);setMessage("Watch record added. This registers the device in your account; Bluetooth pairing and live sync are not enabled yet.");}
    setBusy(false);
  }
  async function signOut(){await supabase.auth.signOut();setMessage("");}
  if(loading)return <main className="shell"><div className="topbar"><div className="brand"><div className="brandmark">A</div>AnishOS</div><span className="topmeta">INITIALIZING SECURE SESSION</span></div></main>;
  return <main className="shell">
    <header className="topbar"><a className="brand" href="#"><span className="brandmark">A</span><span>AnishOS</span></a><span className="topmeta">WEAR OS COMPANION · PRIVATE BETA</span>{user?<button className="btn ghost" onClick={signOut}>Sign out</button>:<span className="topmeta">ACCOUNT ACCESS</span>}</header>
    {!user?<section className="authwrap"><div className="panel authcard">
      <span className="eyebrow">YOUR PERSONAL WATCH ECOSYSTEM</span><h1>{mode==="signin"?"Welcome back.":"Create your account."}</h1><p>Sign in to manage your AnishOS companion account and registered watch devices.</p>
      <form onSubmit={authenticate}>
        {mode==="signup"&&<input className="field" value={displayName} onChange={e=>setDisplayName(e.target.value)} placeholder="Display name" aria-label="Display name"/>}
        <input className="field" type="email" autoComplete="email" required value={email} onChange={e=>setEmail(e.target.value)} placeholder="Email address" aria-label="Email address"/>
        <input className="field" type="password" autoComplete={mode==="signin"?"current-password":"new-password"} minLength={8} required value={password} onChange={e=>setPassword(e.target.value)} placeholder="Password (at least 8 characters)" aria-label="Password"/>
        <button className="btn primary" disabled={busy}>{busy?"Please wait…":mode==="signin"?"Sign in securely":"Create account"}</button>
      </form>
      <p style={{marginTop:18}}>{mode==="signin"?"New to AnishOS?":"Already registered?"} <button className="btn ghost" onClick={()=>{setMode(mode==="signin"?"signup":"signin");setError("");setMessage("");}}>{mode==="signin"?"Create account":"Sign in"}</button></p>
      {error&&<div className="error" role="alert">{error}</div>}{message&&<div className="success" role="status">{message}</div>}
    </div></section>:<>
      <section className="hero"><div><span className="eyebrow">ANISHOS CONTROL CENTER</span><h1>Your watch.<br/>Your ecosystem.</h1><p>Welcome, {user.email ?? "AnishOS user"}. Manage your account and keep your Galaxy Watch companion setup in one place.</p></div><span className="statuspill"><span className="dot"/> ACCOUNT AUTHENTICATED</span></section>
      <section className="grid"><div className="stack">
        <article className="panel"><div className="panelhead"><div><span className="eyebrow">OVERVIEW</span><h2 style={{marginTop:7}}>Your AnishOS workspace</h2></div><span className="mono">V 0.1.0</span></div><div className="overview">
          <div className="metric"><span>Registered watches</span><strong>{devices.length}</strong></div><div className="metric"><span>Paired</span><strong>{devices.filter(d=>d.status==="paired").length}</strong></div><div className="metric"><span>Account</span><strong style={{fontSize:16}}>Secure</strong></div>
        </div></article>
        <article className="panel"><div className="panelhead"><div><span className="eyebrow">DEVICE MANAGEMENT</span><h2 style={{marginTop:7}}>Your watches</h2></div><span className="mono">{devices.length} RECORDS</span></div>
          {devices.length===0?<p>No watch has been registered yet. Add your Galaxy Watch4 to create its account record.</p>:devices.map(device=><div className="device" key={device.id}><div className="deviceicon">⌚</div><div className="devicecopy"><strong>{device.device_name}</strong><small>{device.device_model??"Wear OS device"} · {new Date(device.created_at).toLocaleDateString()}</small></div><span className="tag">{device.status.toUpperCase()}</span></div>)}
          <form className="formrow" onSubmit={registerWatch}><input className="field" value={deviceName} onChange={e=>setDeviceName(e.target.value)} aria-label="Watch name" placeholder="Watch name"/><button className="btn primary" disabled={busy}>Add watch</button></form>
          <div className="notice">Adding a watch here creates an account record only. Actual Bluetooth pairing, notification relay and sensor synchronization will be implemented in the next device-integration stage.</div>
        </article>
      </div><aside className="stack">
        <article className="panel"><span className="eyebrow">ECOSYSTEM MODULES</span>
          <div className="feature"><div className="featureicon">◉</div><div><strong>Watch dashboard</strong><small>Wear OS companion app foundation for Galaxy Watch4.</small></div></div>
          <div className="feature"><div className="featureicon">✦</div><div><strong>AnishOS AI</strong><small>Assistant interface planned. No external AI provider or API key is connected yet.</small></div></div>
          <div className="feature"><div className="featureicon">⌁</div><div><strong>Health & activity</strong><small>Health data stays off until explicit device permissions and opt-in sync are implemented.</small></div></div>
          <div className="feature"><div className="featureicon">◈</div><div><strong>Phone connectivity</strong><small>Bluetooth and notification bridge are planned for the companion integration.</small></div></div>
        </article>
        <article className="panel"><span className="eyebrow">ACCOUNT SECURITY</span><h2 style={{marginTop:8}}>Protected by Supabase</h2><p>Each account can access only its own profile and registered device records. Database row-level security is enabled for both tables.</p><div className="statuspill" style={{marginTop:16}}><span className="dot"/> ROW-LEVEL SECURITY ACTIVE</div></article>
      </aside></section>
      {error&&<div className="error" role="alert">{error}</div>}{message&&<div className="success" role="status">{message}</div>}
    </>}
    <footer className="footer"><span>ANISHOS · BUILT BY ANISH ENTERPRISES</span><span>© 2026 · PRIVATE BETA</span></footer>
  </main>;
}
