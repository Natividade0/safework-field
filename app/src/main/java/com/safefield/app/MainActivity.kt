package com.safefield.app

import android.Manifest
import android.app.Activity
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
    private var statusCard: LinearLayout? = null
    private var statusView: TextView? = null
    private var summaryView: TextView? = null
    private var controlsBox: LinearLayout? = null
    private var workersView: TextView? = null
    private var photosView: TextView? = null

    private val fields = mutableMapOf<String, EditText>()
    private val selectedActs = mutableSetOf<String>()
    private val risks = mutableSetOf<String>()
    private val manualRisks = mutableSetOf<String>()
    private val checkStatus = mutableMapOf<Int,String>()
    private val controlStatus = mutableMapOf<String,String>()
    private val workers = mutableListOf<String>()
    private val photos = mutableListOf<Uri>()

    private val riskBank = mapOf(
        "Içamento de carga" to listOf("Raio de fogo/carga suspensa","Queda de objetos","Tombamento","Prensagem"),
        "Trabalho em altura" to listOf("Queda de pessoas","Queda de objetos","Acesso inseguro","Ancoragem inadequada"),
        "Escavação" to listOf("Desmoronamento","Interferência subterrânea","Queda de pessoas","Atropelamento"),
        "Trabalho a quente" to listOf("Incêndio/explosão","Queimadura","Projeção de partículas","Fumos metálicos"),
        "Eletricidade / LOTO" to listOf("Choque elétrico","Arco elétrico","Energia residual","Bloqueio inadequado"),
        "Montagem industrial" to listOf("Prensagem","Corte/perfuração","Queda de objetos","Ergonômicos")
    )
    private val activityDesc = mapOf(
        "Içamento de carga" to "Carga suspensa, área isolada, patolamento e sinaleiro.",
        "Trabalho em altura" to "NR-35, ancoragem, acesso seguro e proteção contra queda.",
        "Escavação" to "Solo, talude/escoramento, interferências e isolamento.",
        "Trabalho a quente" to "Incêndio, queimadura, projeção, fumos e vigia de fogo.",
        "Eletricidade / LOTO" to "Bloqueio, etiqueta, ausência de tensão e energia residual.",
        "Montagem industrial" to "Prensagem, corte, queda de objetos e ergonomia."
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 10)
        }
        buildShell(); showHome()
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun bg(c:Int,r:Int=18,s:Int?=null)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();if(s!=null)setStroke(dp(1),s)}
    private fun tv(s:String,sz:Float,c:Int,b:Boolean)=TextView(this).apply{text=s;textSize=sz;setTextColor(c);if(b)setTypeface(Typeface.DEFAULT_BOLD);setPadding(0,dp(2),0,dp(2))}
    private fun card()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=bg(cardColor,20,Color.rgb(39,45,57));setPadding(dp(16),dp(14),dp(16),dp(16))}
    private fun addCard(v:LinearLayout){content.addView(v,LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,0,0,dp(12))})}
    private fun btn(s:String,primary:Boolean,on:()->Unit)=Button(this).apply{text=s;textSize=14f;setTextColor(if(primary)Color.BLACK else textColor);setTypeface(Typeface.DEFAULT_BOLD);background=bg(if(primary)amber else Color.rgb(28,33,43),16,if(primary)null else Color.rgb(49,57,72));setOnClickListener{on()}}
    private fun section(p:LinearLayout,s:String){p.addView(tv(s,18f,amber,true).apply{setPadding(0,dp(6),0,dp(10))})}
    private fun clear(){content.removeAllViews();fields.clear()}
    private fun input(p:LinearLayout,k:String,h:String,multi:Boolean=false):EditText{val e=EditText(this).apply{hint=h;setTextColor(textColor);setHintTextColor(Color.rgb(100,110,125));setSingleLine(!multi);minLines=if(multi)3 else 1;background=bg(panel,14,Color.rgb(45,52,66));setPadding(dp(14),dp(10),dp(14),dp(10));textSize=14f};fields[k]=e;p.addView(tv(h,12f,muted,true));p.addView(e);return e}

    private fun buildShell(){
        root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(dark)}
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(18),dp(18),dp(18),dp(18));setBackgroundColor(shell)}
        val title=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}
        title.addView(tv("SafeField",30f,amber,true)); title.addView(tv("Segurança do Trabalho em campo",13f,muted,false))
        header.addView(title,LinearLayout.LayoutParams(0,-2,1f)); header.addView(btn("Menu",false){showHome()})
        val scroll=ScrollView(this); content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(16),dp(18),dp(16),dp(28))}
        scroll.addView(content); root.addView(header); root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f)); setContentView(root)
    }

    private fun showHome(){
        clear(); content.addView(tv("Módulos",27f,textColor,true)); content.addView(tv("Base Kotlin nativa • foco na PT inteligente",14f,muted,false))
        listOf("Permissão de Trabalho","APR","DDS","EPI","Inspeção","Ocorrência","Colaboradores","Dashboard").forEach{m->
            val c=card(); c.addView(tv(m,18f,if(m=="Permissão de Trabalho")amber else textColor,true)); c.addView(tv(if(m=="Permissão de Trabalho")"PT com fluxo guiado, riscos automáticos e PDF nativo" else "Módulo preparado para expansão",13f,muted,false)); c.addView(btn("Abrir",m=="Permissão de Trabalho"){if(m=="Permissão de Trabalho")showPT() else placeholder(m)}); addCard(c)
        }
    }
    private fun placeholder(n:String){clear(); content.addView(tv(n,27f,textColor,true)); val c=card(); c.addView(tv("Módulo preparado para expansão.",15f,textColor,false)); c.addView(btn("Voltar",false){showHome()}); addCard(c)}

    private fun showPT(){
        clear(); selectedActs.clear(); risks.clear(); manualRisks.clear(); checkStatus.clear(); controlStatus.clear(); workers.clear(); photos.clear()
        content.addView(tv("Permissão de Trabalho",27f,textColor,true))
        content.addView(tv("Fluxo inteligente: identifique, analise, controle e emita.",14f,muted,false))
        statusCard=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;background=bg(Color.rgb(72,18,24),22,red);setPadding(dp(16),dp(14),dp(16),dp(14))}
        statusView=tv("PT BLOQUEADA",22f,Color.WHITE,true); summaryView=tv("Preencha os requisitos para liberar.",13f,Color.rgb(255,205,210),false)
        statusCard!!.addView(statusView); statusCard!!.addView(summaryView); addCard(statusCard!!)

        val g=card(); section(g,"1. Identificação do serviço")
        listOf("empresa" to "Empresa / Planta","area" to "Área / Setor","local" to "Local da atividade","responsavel" to "Responsável / Emissor","inicio" to "Data/Hora início","fim" to "Validade / término","executantes" to "Equipe executante").forEach{input(g,it.first,it.second)}
        input(g,"descricao","Descrição detalhada da atividade",true); input(g,"ferramentas","Ferramentas / equipamentos",true); input(g,"produtos","Substâncias / produtos")
        addCard(g)

        val a=card(); section(a,"2. Atividades críticas")
        a.addView(tv("Selecione uma ou mais atividades. O SafeField gera os riscos e controles automaticamente.",13f,muted,false))
        riskBank.keys.forEach{act->a.addView(activityRow(act))}
        val manual=input(a,"manual","Atividade manual / complemento",true)
        a.addView(btn("Analisar atividade manual",false){analyzeManual(manual.text.toString());rebuildRisks()})
        addCard(a)

        val ch=card(); section(ch,"3. Pré-requisitos da liberação")
        ch.addView(tv("Marque Sim, Não ou N/A. Qualquer item como Não bloqueia a PT.",13f,muted,false))
        prereqs().forEachIndexed{i,item->ch.addView(statusRow(item){v->checkStatus[i]=v;refreshStatus()})}
        addCard(ch)

        val rc=card(); section(rc,"4. Análise inteligente de riscos")
        controlsBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL}; rc.addView(controlsBox); addCard(rc)

        val em=card(); section(em,"5. Emergência")
        input(em,"ponto","Ponto de encontro"); input(em,"telefone","Ramal / telefone de emergência"); input(em,"emergencia","Procedimento de emergência",true); addCard(em)

        val wf=card(); section(wf,"6. Equipe e evidências")
        val wi=input(wf,"worker","Nome do trabalhador + função"); wf.addView(btn("Adicionar trabalhador",false){val v=wi.text.toString().trim();if(v.isNotEmpty()){workers.add(v);wi.setText("");updateLists();refreshStatus()}})
        workersView=tv("Nenhum trabalhador adicionado.",13f,muted,false); wf.addView(workersView)
        wf.addView(btn("Adicionar foto",false){openPhotoChooser()}); photosView=tv("Fotos anexadas: 0",13f,muted,false); wf.addView(photosView); addCard(wf)

        val f=card(); section(f,"7. Emissão")
        f.addView(tv("O PDF será gerado de forma nativa, sem WebView e sem impressão.",13f,muted,false)); f.addView(btn("Gerar e compartilhar PDF",true){sharePtPdf()}); f.addView(btn("Voltar ao menu",false){showHome()}); addCard(f)
        rebuildRisks(); updateLists(); refreshStatus()
    }

    private fun activityRow(act:String)=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;background=bg(panel,16,Color.rgb(45,52,66));setPadding(dp(12),dp(10),dp(12),dp(10))
        val cb=CheckBox(context).apply{text=act;textSize=15f;setTextColor(textColor);setTypeface(Typeface.DEFAULT_BOLD);buttonTintList=android.content.res.ColorStateList.valueOf(amber);setOnCheckedChangeListener{_,ok->if(ok)selectedActs.add(act) else selectedActs.remove(act);rebuildRisks()}}
        addView(cb); addView(tv(activityDesc[act] ?: "",12f,muted,false))
    }
    private fun statusRow(label:String,on:(String)->Unit)=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;background=bg(softPanel,16,Color.rgb(52,60,74));setPadding(dp(12),dp(8),dp(12),dp(8))
        addView(tv(label,13f,textColor,false)); val r=RadioGroup(context).apply{orientation=RadioGroup.HORIZONTAL}
        listOf("sim" to "Sim","nao" to "Não","na" to "N/A").forEach{(v,n)->r.addView(RadioButton(context).apply{text=n;setTextColor(textColor);buttonTintList=android.content.res.ColorStateList.valueOf(amber);setOnClickListener{on(v)}})}; addView(r)
    }
    private fun riskCard(risk:String)=LinearLayout(this).apply{
        orientation=LinearLayout.VERTICAL;background=bg(Color.rgb(20,24,32),18,amber);setPadding(dp(14),dp(12),dp(14),dp(12))
        addView(tv("RISCO IDENTIFICADO",11f,amber,true)); addView(tv(risk,17f,textColor,true)); addView(tv("Medidas de controle aplicáveis",12f,muted,false))
        (controlBank[risk]?:listOf("Medida de controle a definir")).forEach{c->addView(statusRow(c){v->controlStatus["$risk|$c"]=v;refreshStatus()})}
    }
    private fun analyzeManual(x:String){val t=x.lowercase(); manualRisks.clear(); if("mont" in t||"tub" in t||"caixa" in t)manualRisks.addAll(listOf("Prensagem","Corte/perfuração","Queda de objetos","Ergonômicos")); if("altura" in t||"andaime" in t||"escada" in t)manualRisks.addAll(listOf("Queda de pessoas","Queda de objetos")); if("sold" in t||"quente" in t||"corte" in t)manualRisks.addAll(listOf("Incêndio/explosão","Queimadura","Projeção de partículas")); if("escav" in t||"vala" in t)manualRisks.addAll(listOf("Desmoronamento","Interferência subterrânea","Atropelamento")); toast("Atividade manual analisada")}
    private fun rebuildRisks(){risks.clear();selectedActs.forEach{risks.addAll(riskBank[it]?: emptyList())};risks.addAll(manualRisks);renderControls();refreshStatus()}
    private fun renderControls(){val box=controlsBox?:return;box.removeAllViews();if(risks.isEmpty()){box.addView(tv("Nenhum risco gerado ainda.",16f,textColor,true));box.addView(tv("Selecione uma atividade crítica ou use a análise manual para montar os riscos e medidas.",13f,muted,false));return};box.addView(tv("${risks.size} risco(s) identificado(s). Revise as medidas antes da emissão.",13f,muted,false));risks.forEach{box.addView(riskCard(it))}}
    private fun refreshStatus(){val pc=prereqs().indices.count{checkStatus[it].isNullOrBlank()};val nc=checkStatus.values.count{it=="nao"};val pend=risks.sumOf{r->(controlBank[r]?:listOf("Medida de controle a definir")).count{c->controlStatus["$r|$c"].isNullOrBlank()}};val blocked=pc>0||nc>0||pend>0||workers.isEmpty();statusCard?.background=bg(if(blocked)Color.rgb(72,18,24) else Color.rgb(16,64,38),22,if(blocked)red else green);statusView?.text=if(blocked)"PT BLOQUEADA" else "PT LIBERADA";summaryView?.text="Atividades: ${selectedActs.size} • Riscos: ${risks.size} • Checklist pendente: $pc • Controles pendentes: $pend • Trabalhadores: ${workers.size} • Fotos: ${photos.size}"}
    private fun updateLists(){workersView?.text=if(workers.isEmpty())"Nenhum trabalhador adicionado." else workers.joinToString("\n"){"• $it"};photosView?.text="Fotos anexadas: ${photos.size}"}
    private fun prereqs()=listOf("APR válida emitida para esta atividade","EPIs específicos inspecionados e aprovados","Treinamento NR em dia para todos os envolvidos","Área isolada e sinalizada adequadamente","Responsável presente no local antes do início")
    private fun v(k:String)=fields[k]?.text?.toString()?.trim().orEmpty()
    private fun label(v:String?)=when(v){"sim"->"[X]";"nao"->"[NÃO]";"na"->"[N/A]";else->"[ ]"}

    private fun sharePtPdf(){val pdf=createPtPdf();val uri=FileProvider.getUriForFile(this,"$packageName.fileprovider",pdf);startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/pdf";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Compartilhar PT em PDF"))}
    private fun createPtPdf():File{val file=File(cacheDir,"SafeField_PT_${System.currentTimeMillis()}.pdf");val doc=PdfDocument();var pageNo=1;var page=newPage(doc,pageNo);var c=page.canvas;var y=70f;fun ensure(h:Float){if(y+h>790f){doc.finishPage(page);pageNo++;page=newPage(doc,pageNo);c=page.canvas;y=70f}};fun line(s:String,b:Boolean=false){val p=Paint(Paint.ANTI_ALIAS_FLAG).apply{color=Color.BLACK;textSize=12f;typeface=if(b)Typeface.DEFAULT_BOLD else Typeface.DEFAULT};wrap(s,82).forEach{ensure(18f);c.drawText(it,38f,y,p);y+=18f}};fun title(s:String){ensure(34f);val p=Paint().apply{color=dark};c.drawRect(30f,y-18f,565f,y+8f,p);p.color=amber;p.textSize=14f;p.typeface=Typeface.DEFAULT_BOLD;c.drawText(s,38f,y,p);y+=28f};title("PERMISSÃO DE TRABALHO");line(statusView?.text?.toString().orEmpty(),true);title("1. Informações gerais");listOf("Empresa" to v("empresa"),"Área" to v("area"),"Local" to v("local"),"Responsável" to v("responsavel"),"Início" to v("inicio"),"Fim" to v("fim"),"Executantes" to v("executantes"),"Descrição" to v("descricao"),"Ferramentas" to v("ferramentas"),"Produtos" to v("produtos")).forEach{line("${it.first}: ${it.second}")};title("2. Checklist");prereqs().forEachIndexed{i,it->line("${label(checkStatus[i])} $it")};title("3. Riscos e controles");if(risks.isEmpty())line("Nenhum risco selecionado.") else risks.forEach{r->line("Risco: $r",true);(controlBank[r]?:listOf("Medida de controle a definir")).forEach{cc->line("${label(controlStatus["$r|$cc"])} $cc")}};title("4. Emergência");line("Ponto: ${v("ponto")}");line("Telefone: ${v("telefone")}");line("Procedimento: ${v("emergencia")}");title("5. Trabalhadores e evidências");if(workers.isEmpty())line("Nenhum trabalhador informado.") else workers.forEach{line("• $it")};line("Fotos anexadas: ${photos.size}");title("6. Encerramento");line("Atividade concluída: ___ Sim  ___ Não");line("Área limpa e segura: ___ Sim  ___ Não");line("Responsável pelo encerramento: ______________________________");doc.finishPage(page);FileOutputStream(file).use{doc.writeTo(it)};doc.close();return file}
    private fun newPage(doc:PdfDocument,n:Int):PdfDocument.Page{val p=doc.startPage(PdfDocument.PageInfo.Builder(595,842,n).create());val c=p.canvas;val paint=Paint(Paint.ANTI_ALIAS_FLAG);paint.color=dark;c.drawRect(0f,0f,595f,48f,paint);paint.color=amber;c.drawRect(0f,0f,10f,842f,paint);paint.color=amber;paint.textSize=20f;paint.typeface=Typeface.DEFAULT_BOLD;c.drawText("SAFEFIELD",36f,31f,paint);paint.color=textColor;paint.textSize=12f;paint.typeface=Typeface.DEFAULT;c.drawText("Segurança do Trabalho em Campo",170f,31f,paint);paint.color=Color.GRAY;paint.textSize=10f;c.drawText("Página $n",500f,825f,paint);return p}
    private fun wrap(s:String,max:Int):List<String>{val out=mutableListOf<String>();var line="";s.replace("\n"," ").split(" ").forEach{w->if((line+" "+w).trim().length>max){out.add(line);line=w}else line=(line+" "+w).trim()};if(line.isNotEmpty())out.add(line);return if(out.isEmpty())listOf("-") else out}
    private fun openPhotoChooser(){startActivityForResult(Intent.createChooser(Intent(Intent.ACTION_GET_CONTENT).apply{type="image/*";addCategory(Intent.CATEGORY_OPENABLE)},"Selecionar foto"),200)}
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==200&&resultCode==RESULT_OK){data?.data?.let{photos.add(it);updateLists();refreshStatus();toast("Foto adicionada")}}}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
}
