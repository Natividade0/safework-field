import React, { useEffect, useMemo, useRef, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { AlertTriangle, ClipboardCheck, FileText, HardHat, Home, ShieldCheck, Users, PackageCheck, Camera, Activity, Download, Plus, Search, WifiOff, Wifi, PenLine, QrCode, Lock, CheckCircle2, XCircle, Clock3 } from 'lucide-react';
import './styles.css';

const DB_NAME = 'safefield-db';
const DB_VERSION = 1;
const STORE = 'records';

const palette = {
  bg: '#0F1117',
  amber: '#F59E0B',
  ok: '#22C55E',
  danger: '#EF4444',
  neutral: '#374151',
};

const modules = [
  { id: 'apr', title: 'APR', subtitle: 'Análise Preliminar de Risco', icon: ClipboardCheck, color: 'amber' },
  { id: 'pt', title: 'PT', subtitle: 'Permissão de Trabalho', icon: ShieldCheck, color: 'ok' },
  { id: 'dds', title: 'DDS', subtitle: 'Diálogo Diário de Segurança', icon: Users, color: 'amber' },
  { id: 'epi', title: 'EPI', subtitle: 'Entrega, CA e estoque', icon: HardHat, color: 'ok' },
  { id: 'inspecao', title: 'Inspeção', subtitle: 'Checklist, foto e NC', icon: Camera, color: 'danger' },
  { id: 'ocorrencia', title: 'Ocorrência', subtitle: 'Acidente e quase-acidente', icon: AlertTriangle, color: 'danger' },
  { id: 'colaborador', title: 'Colaboradores', subtitle: 'ASO, NR e ficha', icon: Users, color: 'neutral' },
  { id: 'dashboard', title: 'Dashboard', subtitle: 'Indicadores da obra', icon: Activity, color: 'amber' },
];

const taskTypes = ['Altura', 'Espaço confinado', 'Elétrico', 'Escavação', 'Içamento', 'Obra civil', 'Trabalho a quente', 'LOTO'];
const hazardBank = {
  Altura: [
    ['Queda de nível diferente', 'Uso de sistema de ancoragem, cinto paraquedista, talabarte e inspeção do acesso'],
    ['Queda de materiais', 'Isolamento da área, rodapé e amarração de ferramentas'],
  ],
  'Espaço confinado': [
    ['Atmosfera perigosa', 'Medição de gases, ventilação, vigia e plano de resgate'],
    ['Deficiência de oxigênio', 'Monitoramento contínuo e liberação formal da entrada'],
  ],
  Elétrico: [
    ['Choque elétrico', 'Bloqueio, teste de ausência de tensão e ferramentas isoladas'],
    ['Arco elétrico', 'EPI compatível, barreiras e autorização NR-10'],
  ],
  Escavação: [
    ['Soterramento', 'Taludamento/escoramento, isolamento e inspeção diária'],
    ['Interferências subterrâneas', 'Consulta de projetos e sondagem prévia'],
  ],
  Içamento: [
    ['Queda de carga', 'Plano de rigging, isolamento e inspeção de acessórios'],
    ['Prensagem', 'Sinaleiro definido e zona de exclusão'],
  ],
  'Obra civil': [
    ['Corte/perfuração', 'Proteção de partes móveis, luvas adequadas e treinamento'],
    ['Desorganização da frente', '5S, rotas livres e segregação de resíduos'],
  ],
  'Trabalho a quente': [
    ['Incêndio', 'Extintor próximo, retirada de inflamáveis e vigia de fogo'],
    ['Projeção de partículas', 'Anteparo, óculos e protetor facial'],
  ],
  LOTO: [
    ['Partida inesperada', 'Bloqueio individual, etiqueta e teste de energia zero'],
    ['Energia residual', 'Alívio, drenagem, aterramento e verificação'],
  ],
};

function openDb() {
  return new Promise((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, DB_VERSION);
    request.onupgradeneeded = () => {
      const db = request.result;
      if (!db.objectStoreNames.contains(STORE)) db.createObjectStore(STORE, { keyPath: 'id' });
    };
    request.onsuccess = () => resolve(request.result);
    request.onerror = () => reject(request.error);
  });
}

async function putRecord(record) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite');
    tx.objectStore(STORE).put(record);
    tx.oncomplete = resolve;
    tx.onerror = () => reject(tx.error);
  });
}

async function getRecords() {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readonly');
    const req = tx.objectStore(STORE).getAll();
    req.onsuccess = () => resolve(req.result.sort((a, b) => b.createdAt.localeCompare(a.createdAt)));
    req.onerror = () => reject(req.error);
  });
}

function id(prefix) {
  return `${prefix.toUpperCase()}-${new Date().toISOString().slice(0,10).replaceAll('-', '')}-${Math.random().toString(36).slice(2, 6).toUpperCase()}`;
}

function Field({ label, value, onChange, placeholder, type = 'text', required = false }) {
  return <label className="field"><span>{label}{required ? ' *' : ''}</span><input type={type} value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} /></label>;
}

function TextArea({ label, value, onChange, placeholder }) {
  return <label className="field"><span>{label}</span><textarea value={value} onChange={e => onChange(e.target.value)} placeholder={placeholder} rows={4} /></label>;
}

function SignaturePad({ onSave }) {
  const canvasRef = useRef(null);
  const [drawing, setDrawing] = useState(false);
  const getPoint = e => {
    const rect = canvasRef.current.getBoundingClientRect();
    const touch = e.touches?.[0];
    return { x: (touch?.clientX ?? e.clientX) - rect.left, y: (touch?.clientY ?? e.clientY) - rect.top };
  };
  const start = e => { setDrawing(true); const p = getPoint(e); const ctx = canvasRef.current.getContext('2d'); ctx.beginPath(); ctx.moveTo(p.x, p.y); };
  const move = e => { if (!drawing) return; e.preventDefault(); const p = getPoint(e); const ctx = canvasRef.current.getContext('2d'); ctx.lineWidth = 3; ctx.lineCap = 'round'; ctx.strokeStyle = '#F59E0B'; ctx.lineTo(p.x, p.y); ctx.stroke(); };
  const end = () => setDrawing(false);
  return <div className="signature"><canvas ref={canvasRef} width="600" height="180" onMouseDown={start} onMouseMove={move} onMouseUp={end} onMouseLeave={end} onTouchStart={start} onTouchMove={move} onTouchEnd={end}/><div className="row"><button className="ghost" onClick={() => canvasRef.current.getContext('2d').clearRect(0,0,600,180)}>Limpar</button><button onClick={() => onSave(canvasRef.current.toDataURL('image/png'))}><PenLine size={18}/> Salvar assinatura</button></div></div>;
}

function App() {
  const [active, setActive] = useState('home');
  const [records, setRecords] = useState([]);
  const [online, setOnline] = useState(navigator.onLine);
  const [query, setQuery] = useState('');
  const [form, setForm] = useState({ type: 'Altura', descricao: '', local: '', responsavel: '', turno: 'Diurno', observacoes: '', colaborador: '', assinatura: '', gravidade: 'moderada' });

  useEffect(() => {
    getRecords().then(setRecords);
    const goOnline = () => setOnline(true);
    const goOffline = () => setOnline(false);
    addEventListener('online', goOnline); addEventListener('offline', goOffline);
    if ('serviceWorker' in navigator) navigator.serviceWorker.register('/sw.js');
    return () => { removeEventListener('online', goOnline); removeEventListener('offline', goOffline); };
  }, []);

  const stats = useMemo(() => ({
    apr: records.filter(r => r.module === 'apr').length,
    ptOpen: records.filter(r => r.module === 'pt' && r.status !== 'Encerrada').length,
    ncOpen: records.filter(r => r.module === 'inspecao' && r.gravidade !== 'leve').length,
    dds: records.filter(r => r.module === 'dds').length,
    epi: records.filter(r => r.module === 'epi').length,
    ocorrencias: records.filter(r => r.module === 'ocorrencia').length,
  }), [records]);

  const save = async (moduleId) => {
    const record = { ...form, id: id(moduleId), module: moduleId, createdAt: new Date().toISOString(), sync: online ? 'pending-cloud' : 'offline', gps: 'capturar no dispositivo', hazards: hazardBank[form.type] ?? [] };
    await putRecord(record);
    const next = await getRecords();
    setRecords(next);
    setForm({ ...form, descricao: '', local: '', observacoes: '', colaborador: '', assinatura: '' });
    setActive('dashboard');
  };

  const exportPdf = () => window.print();

  const filtered = records.filter(r => `${r.id} ${r.module} ${r.descricao} ${r.local} ${r.responsavel} ${r.colaborador}`.toLowerCase().includes(query.toLowerCase()));

  return <main>
    <header className="topbar">
      <div><h1>SafeField</h1><p>Segurança do Trabalho em campo</p></div>
      <div className={online ? 'status online' : 'status offline'}>{online ? <Wifi size={16}/> : <WifiOff size={16}/>} {online ? 'online' : 'offline'}</div>
    </header>

    {active === 'home' && <section className="screen">
      <div className="hero"><div><strong>Operações críticas em até 3 toques</strong><span>APR, PT, DDS, EPI, inspeção e ocorrências com registro local.</span></div><Lock /></div>
      <div className="grid">{modules.map(m => <button className={`module ${m.color}`} key={m.id} onClick={() => setActive(m.id)}><m.icon size={32}/><b>{m.title}</b><span>{m.subtitle}</span></button>)}</div>
    </section>}

    {['apr','pt','dds','epi','inspecao','ocorrencia','colaborador'].includes(active) && <section className="screen">
      <button className="back" onClick={() => setActive('home')}>← Menu</button>
      <h2>{modules.find(m => m.id === active)?.title}</h2>
      <div className="quick-types">{taskTypes.map(t => <button key={t} className={form.type === t ? 'selected' : ''} onClick={() => setForm({...form, type:t})}>{t}</button>)}</div>
      <Field label="Descrição / atividade" required value={form.descricao} onChange={v => setForm({...form, descricao:v})} placeholder="Ex.: montagem de andaime na área 3" />
      <Field label="Local / frente de trabalho" required value={form.local} onChange={v => setForm({...form, local:v})} placeholder="Ex.: pipe rack, setor B" />
      <Field label="Responsável" required value={form.responsavel} onChange={v => setForm({...form, responsavel:v})} placeholder="Nome do TST ou encarregado" />
      <Field label="Colaborador / equipe" value={form.colaborador} onChange={v => setForm({...form, colaborador:v})} placeholder="Nomes ou equipe" />
      {(active === 'inspecao' || active === 'ocorrencia') && <label className="field"><span>Gravidade</span><select value={form.gravidade} onChange={e => setForm({...form, gravidade:e.target.value})}><option value="critica">Crítica</option><option value="moderada">Moderada</option><option value="leve">Leve</option></select></label>}
      <div className="card"><h3>Perigos e controles sugeridos</h3>{(hazardBank[form.type] ?? []).map(([p,c]) => <p key={p}><b>{p}</b><br/><span>{c}</span></p>)}</div>
      <TextArea label="Observações" value={form.observacoes} onChange={v => setForm({...form, observacoes:v})} placeholder="Condições do local, bloqueios, desvios e medidas adicionais" />
      <SignaturePad onSave={img => setForm({...form, assinatura: img})} />
      <button className="primary" onClick={() => save(active)} disabled={!form.descricao || !form.local || !form.responsavel}><CheckCircle2/> Salvar registro offline</button>
    </section>}

    {active === 'dashboard' && <section className="screen">
      <button className="back" onClick={() => setActive('home')}>← Menu</button>
      <h2>Dashboard</h2>
      <div className="kpis"><div><b>{stats.apr}</b><span>APRs</span></div><div><b>{stats.ptOpen}</b><span>PTs abertas</span></div><div><b>{stats.ncOpen}</b><span>NCs pendentes</span></div><div><b>{stats.dds}</b><span>DDS</span></div><div><b>{stats.epi}</b><span>EPIs</span></div><div><b>{stats.ocorrencias}</b><span>Ocorrências</span></div></div>
      <div className="toolbar"><label><Search size={18}/><input value={query} onChange={e => setQuery(e.target.value)} placeholder="Buscar histórico"/></label><button onClick={exportPdf}><Download size={18}/> PDF</button></div>
      <div className="history">{filtered.map(r => <article key={r.id} className="record"><div><b>{r.id}</b><span>{new Date(r.createdAt).toLocaleString('pt-BR')} • {r.module.toUpperCase()} • {r.sync}</span></div><p>{r.descricao || 'Sem descrição'} — {r.local}</p>{r.gravidade === 'critica' ? <XCircle className="danger"/> : <Clock3/>}</article>)}</div>
    </section>}

    <nav className="bottom"><button onClick={() => setActive('home')}><Home/>Menu</button><button onClick={() => setActive('apr')}><Plus/>APR</button><button onClick={() => setActive('pt')}><FileText/>PT</button><button onClick={() => setActive('dashboard')}><Activity/>Painel</button><button onClick={() => alert('QR code de obra e colaborador preparado para integração.')}><QrCode/>QR</button></nav>
  </main>;
}

createRoot(document.getElementById('root')).render(<App />);
