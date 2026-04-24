/* =============================================================
   Udaan Bharat — app.js  (for index.html only)
   Backend: Spring Boot on http://localhost:8080
   Endpoints:
     POST /api/auth/register  → { name, email, password }
     POST /api/auth/login     → { email, password }
   Response: { token, name, email } on success | { error } on fail
============================================================= */

const API = 'http://localhost:8080/api/auth';

/* ── PARTICLES ── */
(function(){
  const container=document.getElementById('particles');
  if(!container)return;
  for(let i=0;i<18;i++){
    const p=document.createElement('div');p.className='p';
    const size=Math.random()*3+1.5;
    p.style.cssText=`width:${size}px;height:${size}px;left:${Math.random()*100}%;bottom:${Math.random()*20}%;animation-duration:${Math.random()*12+8}s;animation-delay:${Math.random()*10}s;`;
    container.appendChild(p);
  }
})();

/* ── TOAST ── */
let toastTimer;
function showToast(msg,type='success'){
  const t=document.getElementById('toast');
  if(!t)return;
  document.getElementById('toastMsg').textContent=msg;
  document.getElementById('toastIcon').textContent=type==='success'?'✅':'❌';
  t.className=`show ${type}`;
  clearTimeout(toastTimer);
  toastTimer=setTimeout(()=>t.className='',3500);
}

/* ── PANEL MANAGEMENT ── */
function openPanel(type){
  const hero=document.getElementById('hero');
  if(hero)hero.style.display='none';
  ['login','register'].forEach(p=>{
    const el=document.getElementById(p+'Overlay')||document.getElementById(p+'Panel');
    if(el)el.classList.remove('active','show');
  });
  clearErrors();
  const panel=document.getElementById(type+'Overlay')||document.getElementById(type+'Panel');
  if(panel)panel.classList.add('active','show');
}

function closePanels(){
  const hero=document.getElementById('hero');
  if(hero)hero.style.display='block';
  ['login','register'].forEach(p=>{
    const el=document.getElementById(p+'Overlay')||document.getElementById(p+'Panel');
    if(el)el.classList.remove('active','show');
  });
  clearErrors();
}

/* ── FORM VALIDATION ── */
function clearErrors(){
  document.querySelectorAll('.field-err,.fe').forEach(e=>{e.textContent='';e.classList.remove('show')});
  document.querySelectorAll('.field input').forEach(i=>i.classList.remove('error-field','err'));
}

function showFieldError(errId,msg){
  const el=document.getElementById(errId);
  if(!el)return;
  el.textContent=msg;el.classList.add('show');
  el.closest('.field')?.querySelector('input')?.classList.add('error-field','err');
}

/* ── SESSION ── */
function saveSession(token,name,email){
  localStorage.setItem('ub_token',token);
  localStorage.setItem('ub_name',name);
  if(email)localStorage.setItem('ub_email',email);
}
function clearSession(){
  localStorage.removeItem('ub_token');
  localStorage.removeItem('ub_name');
  localStorage.removeItem('ub_email');
}
function getSession(){
  return{token:localStorage.getItem('ub_token'),name:localStorage.getItem('ub_name')};
}

/* ── UI STATE ── */
function setLoggedIn(name){
  const loginBtn=document.getElementById('navLoginBtn');
  const pill=document.getElementById('userPill');
  if(loginBtn)loginBtn.style.display='none';
  if(pill)pill.classList.add('show');
  const av=document.getElementById('userAvatar');
  const nm=document.getElementById('userNameDisplay');
  if(av)av.textContent=name.charAt(0).toUpperCase();
  if(nm)nm.textContent=name;
  document.querySelectorAll('.auth-only').forEach(el=>el.classList.add('show'));
  closePanels();
}
function setLoggedOut(){
  const loginBtn=document.getElementById('navLoginBtn');
  const pill=document.getElementById('userPill');
  if(loginBtn)loginBtn.style.display='';
  if(pill)pill.classList.remove('show');
  document.querySelectorAll('.auth-only').forEach(el=>el.classList.remove('show'));
}
function logout(){
  clearSession();
  setLoggedOut();
  showToast('Logged out successfully.');
}

/* ── EYE TOGGLE ── */
function toggleEye(inputId,btn){
  const inp=document.getElementById(inputId);
  if(!inp)return;
  const show=inp.type==='password';
  inp.type=show?'text':'password';
  btn.textContent=show?'🙈':'👁️';
}

/* ── REGISTER ── */
async function handleRegister(){
  clearErrors();
  const name=document.getElementById('regName').value.trim();
  const email=document.getElementById('regEmail').value.trim();
  const password=document.getElementById('regPassword').value;
  const emailRx=/^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  let ok=true;
  if(!name){showFieldError('regNameErr','Name is required');ok=false}
  if(!email||!emailRx.test(email)){showFieldError('regEmailErr','Enter a valid email');ok=false}
  if(password.length<6){showFieldError('regPasswordErr','Minimum 6 characters');ok=false}
  if(!ok)return;
  const btn=document.getElementById('registerBtn');
  btn.disabled=true;btn.textContent='Registering…';
  try{
    const res=await fetch(`${API}/register`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({name,email,password})});
    const data=await res.json();
    if(!res.ok){showToast(data.error||'Registration failed','error')}
    else{
      showToast('Registration successful! Please login 🎉');
      document.getElementById('regName').value='';
      document.getElementById('regEmail').value='';
      document.getElementById('regPassword').value='';
      setTimeout(()=>openPanel('login'),1800);
    }
  }catch(e){showToast('Cannot reach server. Is Spring Boot running?','error')}
  finally{btn.disabled=false;btn.textContent='Register'}
}

/* ── LOGIN ── */
async function handleLogin(){
  clearErrors();
  const email=document.getElementById('loginEmail').value.trim();
  const password=document.getElementById('loginPassword').value;
  const emailRx=/^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  let ok=true;
  if(!email||!emailRx.test(email)){showFieldError('loginEmailErr','Enter a valid email');ok=false}
  if(!password){showFieldError('loginPasswordErr','Password is required');ok=false}
  if(!ok)return;
  const btn=document.getElementById('loginBtn');
  btn.disabled=true;btn.textContent='Logging in…';
  try{
    const res=await fetch(`${API}/login`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email,password})});
    const data=await res.json();
    if(!res.ok){showToast(data.error||'Invalid email or password','error')}
    else{
      saveSession(data.token,data.name,email);
      showToast(`Welcome, ${data.name}! ✈️`);
      // Redirect to dashboard after toast shows
      setTimeout(()=>window.location.href='dashboard.html',900);
    }
  }catch(e){showToast('Cannot reach server. Is Spring Boot running?','error')}
  finally{btn.disabled=false;btn.textContent='Login'}
}

/* ── KEYBOARD SHORTCUTS ── */
document.addEventListener('DOMContentLoaded',()=>{
  const lp=document.getElementById('loginPassword');
  const rp=document.getElementById('regPassword');
  if(lp)lp.addEventListener('keydown',e=>{if(e.key==='Enter')handleLogin()});
  if(rp)rp.addEventListener('keydown',e=>{if(e.key==='Enter')handleRegister()});
});

/* ── RESTORE SESSION ON LOAD ── */
(function(){
  const{token,name}=getSession();
  if(token&&name)setLoggedIn(name);
})();

/* ── CLOSE OVERLAY ON BACKGROUND CLICK ── */
window.addEventListener('click',e=>{
  if(e.target.classList.contains('panel-overlay')||e.target.classList.contains('ov')){
    closePanels();
  }
});