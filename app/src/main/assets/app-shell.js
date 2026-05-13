const shellApp=document.getElementById('app');
function shellCloseMenu(){document.body.classList.remove('menu-open')}
function shellToggleMenu(){document.body.classList.toggle('menu-open')}
function shellModuleCard(i,t,s,b){return `<div class="module" onclick="openModule('${t}')"><div class="top"><div class="ico">${i}</div><div><div class="name">${t}</div><div class="muted">${s}</div></div></div><div class="badge">${b}</div></div>`}
function home(){shellCloseMenu();shellApp.innerHTML=`<div class="hero"><b>SafeField</b><span>Segurança do Trabalho em campo. Use a tela inicial ou o menu lateral.</span></div><div class="kpis"><div class="kpi"><b>${count('APR')}</b><span>APR emitidas</span></div><div class="kpi"><b>${count('PT')}</b><span>PTs registradas</span></div></div><div class="title">Módulos</div><div class="grid">${shellModuleCard('A','APR','Análise preliminar','Riscos')} ${shellModuleCard('P','PT','Permissão universal','Matriz')} ${shellModuleCard('D','DDS','Diálogo diário','Presença')} ${shellModuleCard('E','EPI','CA, estoque e validade','Controle')} ${shellModuleCard('I','Inspeção','Checklist e NC','Campo')} ${shellModuleCard('O','Ocorrência','Incidente e acidente','Registro')} ${shellModuleCard('C','Colaboradores','ASO e treinamentos','Equipe')} ${shellModuleCard('K','Dashboard','Indicadores da obra','Gestão')}</div>`}
function openModule(m){shellCloseMenu();if(m==='PT')return openPT();if(m==='APR')return apr();if(m==='DDS')return dds();if(m==='EPI')return epi();if(m==='Inspeção')return inspection();if(m==='Ocorrência')return occurrence();if(m==='Colaboradores')return worker();if(m==='Dashboard')return dashboard()}
window.home=home;
window.openModule=openModule;
window.shellToggleMenu=shellToggleMenu;
home();
