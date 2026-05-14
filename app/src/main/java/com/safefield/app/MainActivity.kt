package com.safefield.app

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val amber = Color.rgb(245,158,11)
    private val dark = Color.rgb(15,17,23)
    private val shell = Color.rgb(5,7,11)
    private val panel = Color.rgb(18,22,30)
    private val cardColor = Color.rgb(24,28,36)
    private val softPanel = Color.rgb(31,37,48)
    private val textColor = Color.rgb(230,237,243)
    private val muted = Color.rgb(156,163,175)
    private val green = Color.rgb(34,197,94)
    private val red = Color.rgb(239,68,68)

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private val fields = mutableMapOf<String, EditText>()
    private val data = mutableMapOf<String, String>()
    private val selectedActs = mutableSetOf<String>()
    private val risks = mutableSetOf<String>()
    private val manualRisks = mutableSetOf<String>()
    private val checkStatus = mutableMapOf<Int,String>()
    private val controlStatus = mutableMapOf<String,String>()
    private val workers = mutableListOf<String>()
    private val photos = mutableListOf<Uri>()
    private val emittedPts = mutableListOf<String>()

    private val dtFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR"))
    private val prefs by lazy { getSharedPreferences("safefield_pt", MODE_PRIVATE) }

    private val riskBank = mapOf(
        "Içamento de carga" to listOf("Raio de fogo/carga suspensa","Queda de objetos","Tombamento","Prensagem"),
        "Trabalho em altura" to listOf("Queda de pessoas","Queda de objetos","Acesso inseguro","Ancoragem inadequada"),
        "Escavação" to listOf("Desmoronamento","Interferência subterrânea","Queda de pessoas","Atropelamento"),
        "Trabalho a quente" to listOf("Incêndio/explosão","Queimadura","Projeção de partículas","Fumos metálicos"),
        "Eletricidade / LOTO" to listOf("Choque elétrico","Arco elétrico","Energia residual","Bloqueio inadequado"),
        "Montagem industrial" to listOf("Prensagem","Corte/perfuração","Queda de objetos","Ergonômicos")
    )
    private val controlBank = mapOf(
        "Raio de fogo/carga suspensa" to listOf("Isolar área de içamento","Proibir permanência sob carga","Comunicação com operador e sinaleiro"),
        "Queda de objetos" to listOf("Isolamento inferior","Amarração de ferramentas","Capacete com jugular"),
        "Tombamento" to listOf("Verificar base/patolamento","Avaliar solo e nivelamento","Respeitar capacidade do equipamento"),
        "Prensagem" to listOf("Manter mãos fora da linha de fogo","Comunicação entre executantes","Usar luvas adequadas"),
        "Queda de pessoas" to listOf("Cinto tipo paraquedista","Ancoragem válida","Acesso seguro"),
        "Acesso inseguro" to listOf("Inspecionar escadas/andaimes","Manter circulação desobstruída","Bloquear acesso não autorizado"),
        "Ancoragem inadequada" to listOf("Verificar ponto de ancoragem","Usar talabarte adequado","Registrar liberação do acesso"),
        "Desmoronamento" to listOf("Taludamento ou escoramento","Afastar material da borda","Inspecionar solo"),
        "Interferência subterrânea" to listOf("Consultar interferências","Escavação manual próxima a redes","Sinalizar interferências"),
        "Atropelamento" to listOf("Sinalização de tráfego","Colete refletivo","Rotas separadas"),
        "Incêndio/explosão" to listOf("Remover inflamáveis","Extintor no local","Vigia de fogo"),
        "Queimadura" to listOf("Luvas de raspa","Proteção facial","Delimitar área quente"),
        "Projeção de partículas" to listOf("Óculos de segurança","Protetor facial","Anteparo físico"),
        "Fumos metálicos" to listOf("Ventilação/exaustão","Máscara adequada","Avaliar exposição"),
        "Choque elétrico" to listOf("Bloqueio e etiquetagem","Teste de ausência de tensão","Ferramenta isolada"),
        "Arco elétrico" to listOf("EPI NR-10 adequado","Distância segura","Painel bloqueado"),
        "Energia residual" to listOf("Aliviar energia residual","Teste após bloqueio","Controle de religamento"),
        "Bloqueio inadequado" to listOf("Cadeado individual","Etiqueta identificada","Lista de pontos bloqueados"),
        "Corte/perfuração" to listOf("Luvas adequadas","Ferramenta em bom estado","Manter proteção da ferramenta"),
        "Ergonômicos" to listOf("Revezamento","Postura adequada","Uso de apoio mecânico")
    )

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        loadDraft()
        ensureSmartDefaults()
        buildShell(); showHome()
    }

    override fun onPause() { super.onPause(); saveFields(); saveDraft() }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun bg(c:Int,r:Int=18,s:Int?=null)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();if(s!=null)setStroke(dp(1),s)}
    private fun tv(s:String,sz:Float,c:Int,b:Boolean)=TextView(this).apply{text=s;textSize=sz;setTextColor(c);if(b)setTypeface(Typeface.DEFAULT_BOLD);setPadding(0,dp(3),0,dp(3))}
    private fun card()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=bg(cardColor,22,Color.rgb(39,45,57));setPadding(dp(16),dp(14),dp(16),dp(16))}
    private fun addCard(v:LinearLayout){content.addView(v,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(12))})}
    private fun btn(s:String,primary:Boolean,on:()->Unit)=Button(this).apply{text=s;textSize=14f;setTextColor(if(primary)Color.BLACK else textColor);setTypeface(Typeface.DEFAULT_BOLD);background=bg(if(primary)amber else Color.rgb(28,33,43),16,if(primary)null else Color.rgb(49,57,72));setOnClickListener{saveFields();saveDraft();on()}}
    private fun clear(){content.removeAllViews();fields.clear()}
    private fun input(p:LinearLayout,k:String,h:String,multi:Boolean=false):EditText{val e=EditText(this).apply{hint=h;setText(data[k] ?: "");setTextColor(textColor);setHintTextColor(Color.rgb(100,110,125));setSingleLine(!multi);minLines=if(multi)3 else 1;background=bg(panel,14,Color.rgb(45,52,66));setPadding(dp(14),dp(10),dp(14),dp(10));textSize=14f};fields[k]=e;p.addView(tv(h,12f,muted,true));p.addView(e);return e}
    private fun saveFields(){fields.forEach{(k,e)->data[k]=e.text.toString().trim()}}

    private fun enc(list: Collection<String>) = list.joinToString("||")
    private fun dec(s: String?) = s?.split("||")?.filter { it.isNotBlank() } ?: emptyList()
    private fun saveDraft(){
        prefs.edit().apply{
            data.forEach{(k,v)->putString("data_$k",v)}
            putString("selectedActs", enc(selectedActs)); putString("manualRisks", enc(manualRisks)); putString("workers", enc(workers)); putString("emitted", enc(emittedPts))
            checkStatus.forEach{(k,v)->putString("check_$k",v)}
            controlStatus.forEach{(k,v)->putString("control_$k",v)}
        }.apply()
    }
    private fun loadDraft(){
        data.clear(); selectedActs.clear(); manualRisks.clear(); workers.clear(); emittedPts.clear(); checkStatus.clear(); controlStatus.clear()
        prefs.all.forEach{(k,v)-> if(k.startsWith("data_") && v is String) data[k.removePrefix("data_")]=v }
        selectedActs.addAll(dec(prefs.getString("selectedActs", ""))); manualRisks.addAll(dec(prefs.getString("manualRisks", ""))); workers.addAll(dec(prefs.getString("workers", ""))); emittedPts.addAll(dec(prefs.getString("emitted", "")))
        prefs.all.forEach{(k,v)-> if(k.startsWith("check_") && v is String) checkStatus[k.removePrefix("check_").toIntOrNull() ?: -1]=v }
        prefs.all.forEach{(k,v)-> if(k.startsWith("control_") && v is String) controlStatus[k.removePrefix("control_")]=v }
        rebuildRisks()
    }
    private fun clearDraft(){ prefs.edit().clear().apply(); data.clear(); selectedActs.clear(); risks.clear(); manualRisks.clear(); checkStatus.clear(); controlStatus.clear(); workers.clear(); photos.clear(); ensureSmartDefaults(); toast("Rascunho limpo"); showPT() }

    private fun nowText(): String = dtFormat.format(Date())
    private fun parseDate(value: String?): Date? = try { if (value.isNullOrBlank()) null else dtFormat.parse(value) } catch (_: Exception) { null }
    private fun hoursFromValidity(): Int = data["validade"]?.filter { it.isDigit() }?.toIntOrNull() ?: 8
    private fun calculateEnd(hours: Int): String { val cal = Calendar.getInstance(); cal.time = parseDate(data["inicio"]) ?: Date(); cal.add(Calendar.HOUR_OF_DAY, hours); return dtFormat.format(cal.time) }
    private fun ensureSmartDefaults() { if (data["inicio"].isNullOrBlank()) data["inicio"] = nowText(); if (data["validade"].isNullOrBlank()) data["validade"] = "8h"; if (data["fim"].isNullOrBlank()) data["fim"] = calculateEnd(hoursFromValidity()) }
    private fun useNow() { data["inicio"] = nowText(); data["fim"] = calculateEnd(hoursFromValidity()); saveDraft(); showDataStep() }
    private fun applyValidity(hours: Int) { data["validade"] = "${hours}h"; if (data["inicio"].isNullOrBlank()) data["inicio"] = nowText(); data["fim"] = calculateEnd(hours); saveDraft(); showDataStep() }
    private fun pickDateTime(key: String) { val cal = Calendar.getInstance(); parseDate(data[key])?.let { cal.time = it }; DatePickerDialog(this, { _, y, m, d -> cal.set(Calendar.YEAR, y); cal.set(Calendar.MONTH, m); cal.set(Calendar.DAY_OF_MONTH, d); TimePickerDialog(this, { _, h, min -> cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min); data[key] = dtFormat.format(cal.time); if (key == "inicio" && !data["validade"].isNullOrBlank()) data["fim"] = calculateEnd(hoursFromValidity()); saveDraft(); showDataStep() }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show() }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show() }

    private fun buildShell(){ root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(dark)}; val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(18));setBackgroundColor(shell)}; val title=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; title.addView(tv("SafeField",30f,amber,true)); title.addView(tv("Segurança do Trabalho em campo",13f,muted,false)); header.addView(title,LinearLayout.LayoutParams(0,-2,1f)); header.addView(btn("Menu",false){showHome()}); val scroll=ScrollView(this); content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(18),dp(16),dp(28))}; scroll.addView(content); root.addView(header); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f)); setContentView(root) }

    private fun showHome(){ clear(); content.addView(tv("Módulos",27f,textColor,true)); content.addView(tv("SafeField nativo • versão Kotlin",14f,muted,false)); listOf("Permissão de Trabalho","APR","DDS","EPI","Inspeção","Ocorrência","Colaboradores","Dashboard").forEach{m-> val c=card(); c.addView(tv(m,18f,if(m=="Permissão de Trabalho")amber else textColor,true)); c.addView(tv(if(m=="Permissão de Trabalho")"Central guiada da PT, rascunho automático e PDF nativo" else "Módulo preparado para expansão",13f,muted,false)); c.addView(btn("Abrir",m=="Permissão de Trabalho"){if(m=="Permissão de Trabalho")showPT() else placeholder(m)}); addCard(c) } }
    private fun placeholder(n:String){clear(); content.addView(tv(n,27f,textColor,true)); val c=card(); c.addView(tv("Módulo preparado para expansão.",15f,textColor,false)); c.addView(btn("Voltar",false){showHome()}); addCard(c)}

    private fun showPT(){ saveFields(); ensureSmartDefaults(); saveDraft(); clear(); rebuildRisks(); content.addView(tv("Central da PT",28f,textColor,true)); content.addView(tv("Rascunho salvo automaticamente. Preencha por etapas.",14f,muted,false)); addCard(statusPanel()); addCard(stepCard("1", "Dados do serviço", "Data/hora sugerida, validade editável e identificação.", dataScore(), { showDataStep() })); addCard(stepCard("2", "Atividades e riscos", "Seleção de atividades, análise manual, riscos e controles.", riskScore(), { showRiskStep() })); addCard(stepCard("3", "Checklist de liberação", "Pré-requisitos com Sim, Não e N/A.", checklistScore(), { showChecklistStep() })); addCard(stepCard("4", "Equipe e evidências", "Trabalhadores envolvidos e fotos do local.", teamScore(), { showTeamStep() })); val emit=card(); emit.addView(tv("Emitir documento",18f,amber,true)); emit.addView(tv("Revise o status da PT antes de gerar o PDF nativo.",13f,muted,false)); emit.addView(btn("Revisar e gerar PDF",true){showReviewStep()}); emit.addView(btn("Limpar rascunho",false){clearDraft()}); emit.addView(btn("Voltar ao menu",false){showHome()}); addCard(emit); if(emittedPts.isNotEmpty()){val h=card(); h.addView(tv("PTs emitidas neste aparelho",18f,amber,true)); emittedPts.takeLast(5).reversed().forEach{h.addView(tv("• $it",13f,textColor,false))}; addCard(h)} }
    private fun statusPanel():LinearLayout{ val blocked=isBlocked(); val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=bg(if(blocked)Color.rgb(72,18,24) else Color.rgb(16,64,38),24,if(blocked)red else green);setPadding(dp(18),dp(16),dp(18),dp(16))}; c.addView(tv(if(blocked)"PT BLOQUEADA" else "PT LIBERADA",24f,Color.WHITE,true)); c.addView(tv(summary(),13f,Color.rgb(240,240,240),false)); return c }
    private fun stepCard(n:String,title:String,desc:String,score:String,open:()->Unit):LinearLayout{ val c=card(); val top=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}; val badge=TextView(this).apply{text=n;textSize=18f;setTextColor(Color.BLACK);setTypeface(Typeface.DEFAULT_BOLD);gravity=Gravity.CENTER;background=bg(amber,18)}; top.addView(badge,LinearLayout.LayoutParams(dp(42),dp(42))); val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(12),0,0,0)}; box.addView(tv(title,18f,textColor,true)); box.addView(tv(desc,13f,muted,false)); box.addView(tv(score,12f,amber,true)); top.addView(box,LinearLayout.LayoutParams(0,-2,1f)); c.addView(top); c.addView(btn("Abrir etapa",false){open()}); return c }

    private fun dateTimeBlock(parent: LinearLayout) { ensureSmartDefaults(); parent.addView(tv("Data, hora e validade",18f,amber,true)); parent.addView(tv("O app sugere a data/hora atual. A validade é escolhida por você e pode ser editada manualmente.",13f,muted,false)); val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; row1.addView(btn("Agora",false){useNow()}, LinearLayout.LayoutParams(0,-2,1f)); row1.addView(btn("Início",false){pickDateTime("inicio")}, LinearLayout.LayoutParams(0,-2,1f)); row1.addView(btn("Término",false){pickDateTime("fim")}, LinearLayout.LayoutParams(0,-2,1f)); parent.addView(row1); val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; row2.addView(btn("4h",false){applyValidity(4)}, LinearLayout.LayoutParams(0,-2,1f)); row2.addView(btn("8h",false){applyValidity(8)}, LinearLayout.LayoutParams(0,-2,1f)); row2.addView(btn("12h",false){applyValidity(12)}, LinearLayout.LayoutParams(0,-2,1f)); row2.addView(btn("24h",false){applyValidity(24)}, LinearLayout.LayoutParams(0,-2,1f)); parent.addView(row2); input(parent,"inicio","Início da PT"); input(parent,"validade","Validade escolhida/editável"); input(parent,"fim","Término calculado/editável") }

    private fun showDataStep(){ ensureSmartDefaults(); clear(); content.addView(tv("1. Dados do serviço",27f,textColor,true)); val c=card(); listOf("empresa" to "Empresa / Planta","area" to "Área / Setor","local" to "Local da atividade","responsavel" to "Responsável / Emissor").forEach{input(c,it.first,it.second)}; dateTimeBlock(c); input(c,"executantes","Equipe executante"); input(c,"descricao","Descrição detalhada da atividade",true); input(c,"ferramentas","Ferramentas / equipamentos",true); input(c,"produtos","Substâncias / produtos"); c.addView(btn("Salvar e voltar para Central",true){showPT()}); addCard(c) }
    private fun showRiskStep(){clear(); content.addView(tv("2. Atividades e riscos",27f,textColor,true)); val a=card(); a.addView(tv("Atividades críticas",18f,amber,true)); riskBank.keys.forEach{act->a.addView(activityRow(act))}; val manual=input(a,"manual","Atividade manual / complemento",true); a.addView(btn("Analisar atividade manual",false){analyzeManual(manual.text.toString());showRiskStep()}); addCard(a); val r=card(); r.addView(tv("Riscos e controles gerados",18f,amber,true)); val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; r.addView(box); renderControls(box); r.addView(btn("Salvar e voltar para Central",true){showPT()}); addCard(r)}
    private fun showChecklistStep(){clear(); content.addView(tv("3. Checklist de liberação",27f,textColor,true)); val c=card(); c.addView(tv("Qualquer item como Não bloqueia a PT.",13f,muted,false)); prereqs().forEachIndexed{i,item->c.addView(statusRow(item){v->checkStatus[i]=v; saveDraft()})}; c.addView(btn("Salvar e voltar para Central",true){showPT()}); addCard(c)}
    private fun showTeamStep(){clear(); content.addView(tv("4. Equipe e evidências",27f,textColor,true)); val c=card(); val wi=input(c,"worker","Nome do trabalhador + função"); c.addView(btn("Adicionar trabalhador",false){val v=wi.text.toString().trim();if(v.isNotEmpty()){workers.add(v);data["worker"]="";saveDraft();showTeamStep()}}); c.addView(tv(if(workers.isEmpty())"Nenhum trabalhador adicionado." else workers.joinToString("\n"){"• $it"},13f,muted,false)); c.addView(btn("Adicionar foto",false){openPhotoChooser()}); c.addView(tv("Fotos anexadas: ${photos.size}",13f,muted,false)); c.addView(btn("Salvar e voltar para Central",true){showPT()}); addCard(c)}
    private fun showReviewStep(){saveFields(); clear(); rebuildRisks(); content.addView(tv("Revisar e emitir",27f,textColor,true)); addCard(statusPanel()); val c=card(); c.addView(tv("Resumo da PT",18f,amber,true)); c.addView(tv("Empresa: ${v("empresa")}",13f,textColor,false)); c.addView(tv("Local: ${v("local")}",13f,textColor,false)); c.addView(tv("Início: ${v("inicio")}",13f,textColor,false)); c.addView(tv("Validade: ${v("validade")}",13f,textColor,false)); c.addView(tv("Término: ${v("fim")}",13f,textColor,false)); c.addView(tv("Atividades: ${selectedActs.joinToString()}",13f,textColor,false)); c.addView(tv("Riscos: ${risks.size}",13f,textColor,false)); c.addView(tv("Trabalhadores: ${workers.size}",13f,textColor,false)); c.addView(tv("Fotos: ${photos.size}",13f,textColor,false)); validationMessages().forEach{c.addView(tv("⚠ $it",13f,red,true))}; c.addView(btn("Gerar e compartilhar PDF",true){emitPdfIfValid()}); c.addView(btn("Voltar para Central",false){showPT()}); addCard(c)}

    private fun activityRow(act:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=bg(panel,16,Color.rgb(45,52,66));setPadding(dp(12),dp(10),dp(12),dp(10));val cb=CheckBox(context).apply{text=act;textSize=15f;setTextColor(textColor);setTypeface(Typeface.DEFAULT_BOLD);buttonTintList=android.content.res.ColorStateList.valueOf(amber);isChecked=selectedActs.contains(act);setOnCheckedChangeListener{_,ok->if(ok)selectedActs.add(act) else selectedActs.remove(act);rebuildRisks();saveDraft()}};addView(cb);addView(tv("Gera riscos e controles automáticos.",12f,muted,false))}
    private fun statusRow(label:String,on:(String)->Unit)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=bg(softPanel,16,Color.rgb(52,60,74));setPadding(dp(12),dp(8),dp(12),dp(8));addView(tv(label,13f,textColor,false));val r=RadioGroup(context).apply{orientation=RadioGroup.HORIZONTAL};listOf("sim" to "Sim","nao" to "Não","na" to "N/A").forEach{(v,n)->r.addView(RadioButton(context).apply{text=n;setTextColor(textColor);buttonTintList=android.content.res.ColorStateList.valueOf(amber);setOnClickListener{on(v)}})};addView(r)}
    private fun controlRow(risk:String,control:String)=statusRow(control){v->controlStatus["$risk|$control"]=v;saveDraft()}
    private fun renderControls(box:LinearLayout){box.removeAllViews(); if(risks.isEmpty()){box.addView(tv("Nenhum risco gerado. Selecione atividade ou analise manualmente.",14f,muted,false));return}; risks.forEach{r->val rc=card(); rc.background=bg(Color.rgb(20,24,32),18,amber); rc.addView(tv("RISCO IDENTIFICADO",11f,amber,true)); rc.addView(tv(r,17f,textColor,true)); (controlBank[r]?:listOf("Medida de controle a definir")).forEach{c->rc.addView(controlRow(r,c))}; box.addView(rc)}}
    private fun analyzeManual(x:String){val t=x.lowercase(); manualRisks.clear(); if("mont" in t||"tub" in t||"caixa" in t)manualRisks.addAll(listOf("Prensagem","Corte/perfuração","Queda de objetos","Ergonômicos")); if("altura" in t||"andaime" in t||"escada" in t)manualRisks.addAll(listOf("Queda de pessoas","Queda de objetos")); if("sold" in t||"quente" in t||"corte" in t)manualRisks.addAll(listOf("Incêndio/explosão","Queimadura","Projeção de partículas")); if("escav" in t||"vala" in t)manualRisks.addAll(listOf("Desmoronamento","Interferência subterrânea","Atropelamento")); saveDraft(); toast("Atividade manual analisada")}
    private fun rebuildRisks(){risks.clear(); selectedActs.forEach{risks.addAll(riskBank[it]?: emptyList())}; risks.addAll(manualRisks)}
    private fun prereqs()=listOf("APR válida emitida para esta atividade","EPIs específicos inspecionados e aprovados","Treinamento NR em dia para todos os envolvidos","Área isolada e sinalizada adequadamente","Responsável presente no local antes do início")
    private fun pendingChecklist()=prereqs().indices.count{checkStatus[it].isNullOrBlank()}
    private fun negativeChecklist()=checkStatus.values.count{it=="nao"}
    private fun pendingControls()=risks.sumOf{r->(controlBank[r]?:listOf("Medida de controle a definir")).count{c->controlStatus["$r|$c"].isNullOrBlank()}}
    private fun missingRequired()=listOf("empresa" to "Empresa", "local" to "Local", "responsavel" to "Responsável", "inicio" to "Início", "fim" to "Término", "descricao" to "Descrição").filter{data[it.first].isNullOrBlank()}.map{it.second}
    private fun validationMessages(): List<String>{ val out=mutableListOf<String>(); val miss=missingRequired(); if(miss.isNotEmpty()) out.add("Campos obrigatórios pendentes: ${miss.joinToString()}"); if(selectedActs.isEmpty() && manualRisks.isEmpty()) out.add("Informe ao menos uma atividade crítica ou análise manual"); if(workers.isEmpty()) out.add("Adicione ao menos um trabalhador"); if(pendingChecklist()>0) out.add("Checklist ainda possui itens sem resposta"); if(negativeChecklist()>0) out.add("Há item marcado como Não no checklist"); if(pendingControls()>0) out.add("Há medidas de controle sem resposta"); return out }
    private fun isBlocked()=validationMessages().isNotEmpty()
    private fun summary()="Dados: ${dataScore()} • Riscos: ${risks.size} • Checklist pendente: ${pendingChecklist()} • Controles pendentes: ${pendingControls()} • Trabalhadores: ${workers.size} • Fotos: ${photos.size}"
    private fun dataScore()="${listOf("empresa","area","local","responsavel","inicio","validade","fim","descricao").count{!data[it].isNullOrBlank()}}/8 preenchidos"
    private fun riskScore()="${selectedActs.size} atividade(s), ${risks.size} risco(s)"
    private fun checklistScore()="${prereqs().size-pendingChecklist()}/${prereqs().size} respondidos"
    private fun teamScore()="${workers.size} trabalhador(es), ${photos.size} foto(s)"
    private fun v(k:String)=data[k].orEmpty()
    private fun label(v:String?)=when(v){"sim"->"[X]";"nao"->"[NÃO]";"na"->"[N/A]";else->"[ ]"}

    private fun emitPdfIfValid(){ val issues=validationMessages(); if(issues.isNotEmpty()){ toast("PT bloqueada: revise pendências"); showReviewStep(); return }; val stamp="${dtFormat.format(Date())} - ${v("local").ifBlank { "Sem local" }}"; emittedPts.add(stamp); saveDraft(); sharePtPdf() }
    private fun sharePtPdf(){val pdf=createPtPdf();val uri=FileProvider.getUriForFile(this,"$packageName.fileprovider",pdf);startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Compartilhar PT em PDF"))}
    private fun createPtPdf():File{val file=File(cacheDir,"SafeField_PT_${System.currentTimeMillis()}.pdf");val doc=PdfDocument();var pageNo=1;var page=newPage(doc,pageNo);var c=page.canvas;var y=70f;fun ensure(h:Float){if(y+h>790f){doc.finishPage(page);pageNo++;page=newPage(doc,pageNo);c=page.canvas;y=70f}};fun line(s:String,b:Boolean=false){val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.BLACK;textSize=12f;typeface=if(b)Typeface.DEFAULT_BOLD else Typeface.DEFAULT};wrap(s,82).forEach{ensure(18f);c.drawText(it,38f,y,p);y+=18f}};fun title(s:String){ensure(34f);val p=Paint().apply{color=dark};c.drawRect(30f,y-18f,565f,y+8f,p);p.color=amber;p.textSize=14f;p.typeface=Typeface.DEFAULT_BOLD;c.drawText(s,38f,y,p);y+=28f};title("PERMISSÃO DE TRABALHO");line(if(isBlocked())"PT BLOQUEADA" else "PT LIBERADA",true);title("1. Informações gerais");listOf("Empresa" to v("empresa"),"Área" to v("area"),"Local" to v("local"),"Responsável" to v("responsavel"),"Início" to v("inicio"),"Validade" to v("validade"),"Fim" to v("fim"),"Executantes" to v("executantes"),"Descrição" to v("descricao"),"Ferramentas" to v("ferramentas"),"Produtos" to v("produtos")).forEach{line("${it.first}: ${it.second}")};title("2. Checklist");prereqs().forEachIndexed{i,it->line("${label(checkStatus[i])} $it")};title("3. Riscos e controles");if(risks.isEmpty())line("Nenhum risco selecionado.") else risks.forEach{r->line("Risco: $r",true);(controlBank[r]?:listOf("Medida de controle a definir")).forEach{cc->line("${label(controlStatus["$r|$cc"])} $cc")}};title("4. Trabalhadores e evidências");if(workers.isEmpty())line("Nenhum trabalhador informado.") else workers.forEach{line("• $it")};line("Fotos anexadas: ${photos.size}");doc.finishPage(page);FileOutputStream(file).use{doc.writeTo(it)};doc.close();return file}
    private fun newPage(doc:PdfDocument,n:Int):PdfDocument.Page{val p=doc.startPage(PdfDocument.PageInfo.Builder(595,842,n).create());val c=p.canvas;val paint=Paint(Paint.ANTI_ALIAS_FLAG);paint.color=dark;c.drawRect(0f,0f,595f,48f,paint);paint.color=amber;c.drawRect(0f,0f,10f,842f,paint);paint.color=amber;paint.textSize=20f;paint.typeface=Typeface.DEFAULT_BOLD;c.drawText("SAFEFIELD",36f,31f,paint);paint.color=textColor;paint.textSize=12f;paint.typeface=Typeface.DEFAULT;c.drawText("Segurança do Trabalho em Campo",170f,31f,paint);paint.color=Color.GRAY;paint.textSize=10f;c.drawText("Página $n",500f,825f,paint);return p}
    private fun wrap(s:String,max:Int):List<String>{val out=mutableListOf<String>();var line="";s.replace("\n"," ").split(" ").forEach{w->if((line+" "+w).trim().length>max){out.add(line);line=w}else line=(line+" "+w).trim()};if(line.isNotEmpty())out.add(line);return if(out.isEmpty())listOf("-") else out}
    private fun openPhotoChooser(){startActivityForResult(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE)},"Selecionar foto"),200)}
    override fun onActivityResult(requestCode:Int,resultCode:Int,dataIntent:Intent?){super.onActivityResult(requestCode,resultCode,dataIntent);if(requestCode==200&&resultCode==RESULT_OK){dataIntent?.data?.let{photos.add(it);saveDraft();toast("Foto adicionada");showTeamStep()}}}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
