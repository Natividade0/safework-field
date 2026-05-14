(function(){
function text(v){return String(v==null?'':v)}
function list(v){return Array.isArray(v)?v:[]}
function record(r){try{if(!r&&typeof currentPTRecord==='function')r=currentPTRecord()}catch(e){}r=r||window.__lastPdfRecord||{};return r}
function exportPdf(r){
  r=record(r);
  if(!(window.jspdf&&window.jspdf.jsPDF)){
    alert('jsPDF nao carregado. Adicione a biblioteca local para exportar sem paginas brancas.');
    return;
  }
  var jsPDF=window.jspdf.jsPDF;
  var doc=new jsPDF({orientation:'portrait',unit:'mm',format:'a4'});
  var W=210,m=12,y=14;
  function page(){doc.addPage();y=14;head()}
  function check(h){if(y+h>280)page()}
  function line(t,size,bold){check(7);doc.setFont('helvetica',bold?'bold':'normal');doc.setFontSize(size||9);doc.setTextColor(230,237,243);doc.text(text(t)||'-',m,y);y+=6}
  function box(title){check(12);doc.setFillColor(15,17,23);doc.rect(m,y,W-m*2,8,'F');doc.setTextColor(245,158,11);doc.setFont('helvetica','bold');doc.setFontSize(9);doc.text(title,m+2,y+5.5);y+=11}
  function head(){doc.setFillColor(15,17,23);doc.rect(0,0,W,18,'F');doc.setFillColor(245,158,11);doc.rect(0,0,4,18,'F');doc.setTextColor(245,158,11);doc.setFont('helvetica','bold');doc.setFontSize(14);doc.text('SAFEFIELD',m,11);doc.setTextColor(230,237,243);doc.setFontSize(9);doc.text('PERMISSAO DE TRABALHO',W-70,11)}
  function field(k,v){var rows=doc.splitTextToSize(k+': '+(text(v)||'-'),W-m*2);rows.forEach(function(row){line(row,8,false)})}
  head();
  doc.setFillColor(18,22,30);doc.rect(m,24,W-m*2,12,'F');doc.setTextColor(245,158,11);doc.setFont('helvetica','bold');doc.setFontSize(13);doc.text('PERMISSAO DE TRABALHO',m+3,32);y=42;
  if(r.status&&r.status!=='Liberada'){doc.setFillColor(239,68,68);doc.rect(m,y,W-m*2,10,'F');doc.setTextColor(255,255,255);doc.setFontSize(10);doc.text('PT BLOQUEADA - NAO INICIAR ATIVIDADE',m+3,y+7);y+=14}
  box('1. INFORMACOES GERAIS');
  field('Numero / ID',r.id);field('Status',r.status);field('Empresa / Planta',r.empresa);field('Area / Setor',r.area);field('Local',r.local);field('Responsavel',r.resp||r.responsavel);field('Inicio',r.inicio);field('Fim',r.fim);field('Executantes',r.executantes);field('Atividades',list(r.atividades).join(', '));field('Descricao',r.desc||r.descricao);field('Ferramentas',r.ferramentas);field('Produtos',r.produtos);field('Observacoes',r.obs);
  box('2. CHECKLIST DE PRE-REQUISITOS');
  ['APR valida emitida para esta atividade','EPIs especificos inspecionados e aprovados','Treinamento NR em dia','Area isolada e sinalizada','Responsavel presente no local'].forEach(function(q,i){field((r.prereqsChecked&&r.prereqsChecked[i])?'[X]':'[ ]',q)});
  box('3. RISCOS E MEDIDAS DE CONTROLE');
  var riscos=list(r.riscos);if(!riscos.length)field('Riscos','Nenhum risco registrado');
  riscos.forEach(function(risk){field('Risco',risk);try{(riskBank[risk]||[]).forEach(function(c){field('Controle',c)})}catch(e){}});
  box('4. EPIS E EMERGENCIA');
  field('EPIs',list(r.epiExigido).join(', '));field('Ponto de encontro',r.ponto);field('Ramal',r.tel);field('Ambulancia',r.amb);field('Radio',r.radio);field('Emergencia',r.emergencia);
  box('5. ASSINATURAS');
  field('Emissor',r.emissor+' - '+(r.emissorFunc||''));field('Executor lider',r.lider+' - '+(r.liderFunc||''));
  list(r.equipe).forEach(function(w){field('Trabalhador',text(w.nome)+' - '+text(w.funcao)+' - '+text(w.empresa))});
  box('6. ENCERRAMENTO');
  field('Atividade concluida','___ Sim  ___ Nao');field('Area limpa e segura','___ Sim  ___ Nao');field('Responsavel pelo encerramento','____________________________');
  var total=doc.getNumberOfPages();for(var p=1;p<=total;p++){doc.setPage(p);doc.setFillColor(15,17,23);doc.rect(0,287,W,10,'F');doc.setTextColor(107,114,128);doc.setFontSize(7);doc.text('SafeField - Pagina '+p+' / '+total,m,293)}
  var name='PT_'+(r.id||'SafeField')+'.pdf';
  var base64=doc.output('datauristring').split(',')[1];
  if(window.SafeFieldAndroid&&typeof SafeFieldAndroid.shareBase64Pdf==='function')SafeFieldAndroid.shareBase64Pdf(base64,name.replace('.pdf',''));else doc.save(name);
}
window.exportPTPdfJsPDF=exportPdf;
window.printPT=function(r){r=record(r);window.__lastPdfRecord=r;app.innerHTML='<div class="title">PDF da Permissao de Trabalho</div><div class="form-card"><div class="pt-status"><span class="pill ok">jsPDF real</span><span class="pill">sem WebView</span></div><p>PDF gerado em memoria para evitar paginas brancas.</p><button class="primary" onclick="exportPTPdfJsPDF(window.__lastPdfRecord)">Compartilhar PDF direto</button><button class="secondary" onclick="dashboard()">Voltar</button></div>'}
})();
