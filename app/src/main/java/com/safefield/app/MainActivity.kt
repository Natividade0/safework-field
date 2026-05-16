package com.safefield.app

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var repo: PtRepository
    private lateinit var data: PtData
    private val photoRequest: Int = 7001
    private val signatureRequest: Int = 8002
    private var pendingSignatureSave: ((String) -> Unit)? = null
    private var currentBack: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        repo = PtRepository(this)
        data = repo.load()
        if (data.startMillis <= 0L) setNow()
        showHome()
    }

    override fun onPause(): Unit {
        super.onPause()
        repo.save(data)
    }

    override fun onBackPressed(): Unit {
        val back = currentBack
        if (back != null) saveAnd(back) else super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?): Unit {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == signatureRequest) {
            if (resultCode == RESULT_OK) {
                val signature = resultData?.getStringExtra(SignatureActivity.EXTRA_SIGNATURE_B64).orEmpty()
                if (signature.isNotBlank()) {
                    pendingSignatureSave?.invoke(signature)
                    repo.save(data)
                    Toast.makeText(this, "Assinatura salva", Toast.LENGTH_SHORT).show()
                    showTeamEvidence()
                }
            }
            pendingSignatureSave = null
            return
        }
        if (requestCode == photoRequest && resultCode == RESULT_OK) {
            val clip = resultData?.clipData
            if (clip != null) {
                for (i in 0 until clip.itemCount) addPhoto(clip.getItemAt(i).uri)
            } else {
                resultData?.data?.let { addPhoto(it) }
            }
            repo.save(data)
            showTeamEvidence()
        }
    }

    private fun addPhoto(uri: Uri): Unit {
        runCatching { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        data.photoUris.add(uri.toString())
    }

    private fun setNow(): Unit {
        data.startMillis = System.currentTimeMillis()
        data.endMillis = data.startMillis + data.validityHours * 60L * 60L * 1000L
    }

    private fun saveAnd(block: () -> Unit): Unit {
        repo.save(data)
        block()
    }

    private fun screen(title: String, back: (() -> Unit)? = null, body: LinearLayout.() -> Unit): Unit {
        currentBack = back
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Ui.SHELL)
        scroll.isFillViewport = true

        val content = Ui.vbox(this, 20.dp())
        scroll.addView(content, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        val header = Ui.row(this)
        header.gravity = Gravity.CENTER_VERTICAL
        if (back != null) {
            val backButton = Ui.ghostButton(this, "‹")
            backButton.textSize = 24f
            backButton.layoutParams = LinearLayout.LayoutParams(48.dp(), 48.dp())
            backButton.setOnClickListener { saveAnd(back) }
            header.addView(backButton)
        }

        val titleBox = Ui.vbox(this)
        val titleView = Ui.title(this, title, if (title == "SafeField") 30f else 25f)
        titleBox.addView(titleView)
        if (title == "SafeField") titleBox.addView(Ui.label(this, "Segurança do Trabalho em Campo"))
        titleBox.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        header.addView(titleBox)

        content.addView(header.margin(0, 4.dp()))
        content.body()
        Ui.animateIn(content)
        setContentView(scroll)
    }

    private fun showHome(): Unit {
        screen("SafeField") {
            HomeDashboard(
                activity = this@MainActivity,
                data = data,
                openPt = { saveAnd(::showPtCentral) },
                openPlaceholder = { name -> placeholder(name) },
                openPending = { showPtCentral() },
                openHistory = { showPtCentral() },
                openModules = { showHome() }
            ).renderInto(this)
        }
    }

    private fun placeholder(name: String): Unit {
        screen(name, ::showHome) {
            val card = Ui.heroCard(this@MainActivity)
            card.addView(Ui.chip(this@MainActivity, "MÓDULO EM EXPANSÃO", Ui.AMBER))
            card.addView(Ui.title(this@MainActivity, name, 24f).margin(0, 8.dp()))
            card.addView(Ui.label(this@MainActivity, "A base visual e técnica já está pronta para evoluir este módulo sem alterar o núcleo da PT."))
            addView(card.margin(0, 10.dp()))
        }
    }

    private fun showPtCentral(): Unit {
        screen("Permissão de Trabalho", ::showHome) {
            PtCentralDashboard(
                activity = this@MainActivity,
                data = data,
                openData = { saveAnd(::showServiceData) },
                openRisks = { saveAnd(::showRisks) },
                openChecklist = { saveAnd(::showChecklist) },
                openTeam = { saveAnd(::showTeamEvidence) },
                openReview = { saveAnd(::showReview) },
                clearDraft = { confirmClearDraft() }
            ).renderInto(this)
        }
    }

    private fun confirmClearDraft(): Unit {
        AlertDialog.Builder(this)
            .setTitle("Limpar rascunho?")
            .setMessage("Todos os dados, fotos e assinatura salvos serão removidos.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Limpar") { _, _ ->
                data = PtData()
                setNow()
                repo.clear()
                showPtCentral()
            }
            .show()
    }

    private fun showServiceData(): Unit {
        screen("Informações Gerais", ::showPtCentral) {
            addView(documentIntro("1", "Informações Gerais", "Preencha primeiro o que identifica a PT: onde será executado, quem libera, o que será feito e por quanto tempo vale.").margin(0, 8.dp()))

            addView(formBlock("Local da atividade", "Informe a empresa, área e o ponto exato onde a atividade será executada.") {
                addInput("Empresa / Planta", data.company) { data.company = it }
                addInput("Área / Setor", data.area) { data.area = it }
                addInput("Local da atividade", data.place) { data.place = it }
            }.margin(0, 8.dp()))

            addView(formBlock("Responsável e execução", "Registre quem está liberando a PT e qual equipe executará a atividade.") {
                addInput("Responsável / Emissor", data.responsible) { data.responsible = it }
                addInput("Equipe executante", data.teamName) { data.teamName = it }
            }.margin(0, 8.dp()))

            addView(formBlock("Descrição do serviço", "Descreva a atividade de forma objetiva. Isso ajuda a gerar riscos e medidas de controle.") {
                addInput("Descrição detalhada da atividade", data.description, true) { data.description = it }
                addInput("Ferramentas / equipamentos", data.tools, true) { data.tools = it }
                addInput("Substâncias / produtos", data.products, true) { data.products = it }
            }.margin(0, 8.dp()))

            addView(timeCard().margin(0, 8.dp()))

            addView(formBlock("Emergência e observações", "Informe recursos de emergência e qualquer condição especial antes de seguir.") {
                addInput("Ponto de encontro / recurso de emergência", data.emergencyPoint) { data.emergencyPoint = it }
                addInput("Telefone / canal de emergência", data.emergencyPhone) { data.emergencyPhone = it }
                addInput("Procedimento em caso de emergência", data.emergencyProcedure, true) { data.emergencyProcedure = it }
                addInput("Observações gerais", data.observations, true) { data.observations = it }
            }.margin(0, 8.dp()))

            addView(nextButton("Continuar para Lista Geral de Verificação") { showChecklist() }.margin(0, 10.dp()))
        }
    }

    private fun LinearLayout.addInput(label: String, value: String, multi: Boolean = false, onChange: (String) -> Unit): EditText {
        addView(Ui.label(this@MainActivity, label).margin(0, 7.dp()))
        val edit = Ui.input(this@MainActivity, label, multi)
        edit.setText(value)
        edit.addTextChangedListener(SimpleWatcher {
            onChange(edit.text.toString())
            repo.save(data)
        })
        addView(edit.margin(0, 4.dp()))
        return edit
    }

    private fun timeCard(): LinearLayout {
        val card = Ui.heroCard(this)
        val flow = PtFlowEngine.flow(data)
        card.addView(Ui.chip(this, "VIGÊNCIA", Ui.AMBER))
        card.addView(Ui.title(this, "Data, hora e validade", 18f).margin(0, 8.dp()))
        card.addView(Ui.label(this, "Use os atalhos para preencher rápido. O término é calculado pela validade, mas pode ser editado."))
        card.addView(Ui.value(this, "Início: ${Ui.fmt(data.startMillis)}\nValidade: ${data.validityHours}h\nTérmino: ${Ui.fmt(data.endMillis)}").margin(0, 8.dp()))
        card.addView(Ui.value(this, flow.validityLabel, if (flow.status == PtStatus.EXPIRADA) Ui.RED else Ui.AMBER_SOFT).margin(0, 4.dp()))
        flow.validityAlert?.let { card.addView(Ui.value(this, it, Ui.RED).margin(0, 3.dp())) }

        val validities = Ui.row(this)
        listOf(4, 8, 12, 24).forEach { hours ->
            val button = Ui.ghostButton(this, "${hours}h")
            button.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            button.setOnClickListener {
                data.validityHours = hours
                data.endMillis = data.startMillis + hours * 60L * 60L * 1000L
                repo.save(data)
                showServiceData()
            }
            validities.addView(button)
        }
        card.addView(validities.margin(0, 8.dp()))

        val now = Ui.button(this, "Usar horário atual")
        now.setOnClickListener {
            setNow()
            repo.save(data)
            showServiceData()
        }
        card.addView(now.margin(0, 6.dp()))

        val start = Ui.ghostButton(this, "Escolher início")
        start.setOnClickListener {
            pickDateTime(data.startMillis) {
                data.startMillis = it
                data.endMillis = it + data.validityHours * 60L * 60L * 1000L
                repo.save(data)
                showServiceData()
            }
        }
        card.addView(start.margin(0, 4.dp()))

        val end = Ui.ghostButton(this, "Editar término")
        end.setOnClickListener {
            pickDateTime(data.endMillis) {
                data.endMillis = it
                repo.save(data)
                showServiceData()
            }
        }
        card.addView(end.margin(0, 4.dp()))
        return card
    }

    private fun pickDateTime(initial: Long, onPicked: (Long) -> Unit): Unit {
        val cal = Calendar.getInstance()
        cal.timeInMillis = initial
        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                val picked = Calendar.getInstance()
                picked.set(year, month, day, hour, minute, 0)
                picked.set(Calendar.MILLISECOND, 0)
                onPicked(picked.timeInMillis)
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showRisks(): Unit {
        screen("Riscos e Medidas", ::showPtCentral) {
            addView(documentIntro("3", "Riscos e Medidas de Controle", "Selecione a atividade crítica ou descreva a atividade. O app sugere riscos e exige resposta para cada medida de controle.").margin(0, 8.dp()))

            addView(formBlock("Atividades críticas", "Marque uma ou mais atividades aplicáveis ao serviço.") {
                RiskEngine.activities.forEach { activity -> addView(activityRow(activity).margin(0, 5.dp())) }
            }.margin(0, 8.dp()))

            addView(formBlock("Atividade manual", "Se a atividade não estiver na lista, descreva aqui. Exemplo: montagem de tubulação em altura com solda.") {
                addInput("Atividade manual / complemento", data.manualActivity, true) { data.manualActivity = it }
            }.margin(0, 8.dp()))

            val risks = RiskEngine.risksFor(data)
            addView(formBlock("Riscos gerados", if (risks.isEmpty()) "Nenhum risco gerado ainda." else "Confira cada risco e responda as medidas antes de emitir a PT.") {
                if (risks.isEmpty()) {
                    addView(Ui.value(this@MainActivity, "Selecione uma atividade ou descreva a atividade manual para gerar controles.", Ui.AMBER))
                } else {
                    risks.forEach { risk -> addView(riskCard(risk).margin(0, 8.dp())) }
                }
            }.margin(0, 8.dp()))

            addView(nextButton("Continuar para Envolvidos e Assinaturas") { showTeamEvidence() }.margin(0, 10.dp()))
        }
    }

    private fun activityRow(activity: String): CheckBox {
        val check = CheckBox(this)
        check.text = activity
        check.setTextColor(Ui.TEXT)
        check.textSize = 15f
        check.background = Ui.bg(Ui.CARD_SOFT, 16.dp(), if (data.activities.contains(activity)) Ui.AMBER else Ui.BORDER, 1)
        check.setPadding(14.dp(), 10.dp(), 14.dp(), 10.dp())
        check.isChecked = data.activities.contains(activity)
        check.setOnCheckedChangeListener { _, checked ->
            if (checked) data.activities.add(activity) else data.activities.remove(activity)
            repo.save(data)
            showRisks()
        }
        return check
    }

    private fun riskCard(risk: String): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.chip(this, "RISCO", Ui.RED))
        card.addView(Ui.title(this, risk, 18f).margin(0, 8.dp()))
        RiskEngine.controls[risk].orEmpty().forEach { control ->
            val key = RiskEngine.controlKey(risk, control)
            val status = data.controls[key].orEmpty()
            val controlBox = Ui.vbox(this, 10.dp())
            controlBox.background = Ui.bg(Ui.PANEL, 14.dp(), if (status.isBlank()) Ui.AMBER else if (status == RiskEngine.NO) Ui.RED else Ui.BORDER, 1)
            controlBox.addView(Ui.value(this, control, Ui.TEXT))
            val group = answerGroup(status) {
                data.controls[key] = it
                repo.save(data)
            }
            controlBox.addView(group.margin(0, 5.dp()))
            card.addView(controlBox.margin(0, 6.dp()))
        }
        return card
    }

    private fun showChecklist(): Unit {
        screen("Lista Geral", ::showPtCentral) {
            val answered = RiskEngine.checklistItems.count { data.checklist[it]?.isNotBlank() == true }
            addView(documentIntro("2", "Lista Geral de Verificação", "Responda item por item antes da atividade. Qualquer resposta NÃO bloqueia a emissão da PT.").margin(0, 8.dp()))
            addView(Ui.progress(this@MainActivity, answered, RiskEngine.checklistItems.size).margin(0, 8.dp()))
            addView(Ui.value(this@MainActivity, "$answered de ${RiskEngine.checklistItems.size} itens respondidos", Ui.AMBER_SOFT).margin(0, 5.dp()))

            RiskEngine.checklistItems.forEachIndexed { index, item ->
                addView(checklistCard(index + 1, item).margin(0, 7.dp()))
            }
            addView(nextButton("Continuar para Riscos e Medidas") { showRisks() }.margin(0, 10.dp()))
        }
    }

    private fun checklistCard(number: Int, item: String): LinearLayout {
        val card = Ui.card(this)
        val current = data.checklist[item].orEmpty()
        val row = Ui.row(this)
        val badge = TextView(this)
        badge.text = number.toString().padStart(2, '0')
        badge.gravity = Gravity.CENTER
        badge.setTextColor(if (current.isBlank()) Ui.AMBER else if (current == RiskEngine.NO) Ui.RED else Ui.GREEN)
        badge.background = Ui.bg(Ui.PANEL, 14.dp(), if (current.isBlank()) Ui.AMBER else if (current == RiskEngine.NO) Ui.RED else Ui.GREEN, 1)
        row.addView(badge, LinearLayout.LayoutParams(42.dp(), 42.dp()))
        val label = Ui.value(this, item, if (current == RiskEngine.NO) Ui.RED else Ui.TEXT)
        label.setPadding(12.dp(), 0, 0, 0)
        label.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(label)
        card.addView(row)
        val group = answerGroup(current) {
            data.checklist[item] = it
            repo.save(data)
        }
        card.addView(group.margin(0, 8.dp()))
        return card
    }

    private fun answerGroup(current: String, onChange: (String) -> Unit): RadioGroup {
        val group = RadioGroup(this)
        group.orientation = RadioGroup.HORIZONTAL
        listOf(RiskEngine.YES, RiskEngine.NO, RiskEngine.NA).forEach { label ->
            val option = RadioButton(this)
            option.text = label
            option.setTextColor(if (label == RiskEngine.NO && current == label) Ui.RED else Ui.TEXT)
            option.id = View.generateViewId()
            option.tag = label
            option.isChecked = current == label
            group.addView(option)
        }
        group.setOnCheckedChangeListener { radioGroup, checkedId ->
            val selected = radioGroup.findViewById<RadioButton>(checkedId)?.tag as? String
            if (selected != null) onChange(selected)
        }
        return group
    }

    private fun showTeamEvidence(): Unit {
        screen("Assinaturas", ::showPtCentral) {
            addView(documentIntro("5", "Envolvidos e Assinaturas", "Registre quem vai executar o serviço. A PT só fica liberada quando o responsável e todos os envolvidos assinarem.").margin(0, 8.dp()))

            addView(formBlock("Responsável pela liberação", "Assinatura do emissor ou responsável pela PT.") {
                addView(signatureCard().margin(0, 4.dp()))
            }.margin(0, 8.dp()))

            addView(formBlock("Adicionar envolvido", "Informe nome e função. Depois colete a assinatura individual.") {
                val workerName = Ui.input(this@MainActivity, "Nome do envolvido")
                val workerRole = Ui.input(this@MainActivity, "Função / empresa")
                addView(workerName.margin(0, 4.dp()))
                addView(workerRole.margin(0, 4.dp()))
                val addWorker = Ui.button(this@MainActivity, "Adicionar envolvido")
                addWorker.setOnClickListener {
                    if (workerName.text.isBlank()) {
                        Toast.makeText(this@MainActivity, "Informe o nome do envolvido", Toast.LENGTH_SHORT).show()
                    } else {
                        data.workers.add(Worker(workerName.text.toString(), workerRole.text.toString()))
                        repo.save(data)
                        showTeamEvidence()
                    }
                }
                addView(addWorker.margin(0, 8.dp()))
            }.margin(0, 8.dp()))

            addView(formBlock("Lista de envolvidos", "Cada envolvido deve assinar antes da emissão.") {
                if (data.workers.isEmpty()) {
                    addView(Ui.value(this@MainActivity, "Nenhum envolvido adicionado.", Ui.RED))
                    addView(Ui.label(this@MainActivity, "Adicione os trabalhadores e colete a assinatura de cada um."))
                } else {
                    data.workers.forEachIndexed { index, worker -> addView(workerCard(index, worker).margin(0, 6.dp())) }
                }
            }.margin(0, 8.dp()))

            addView(formBlock("Evidências fotográficas", "Anexe fotos do local, isolamento, equipamentos ou condição da atividade.") {
                addView(evidenceCard().margin(0, 4.dp()))
            }.margin(0, 8.dp()))

            addView(nextButton("Revisar PT") { showReview() }.margin(0, 10.dp()))
        }
    }

    private fun workerCard(index: Int, worker: Worker): LinearLayout {
        val card = Ui.card(this)
        val top = Ui.row(this)
        val infoBox = Ui.vbox(this)
        infoBox.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        infoBox.addView(Ui.value(this, worker.name, Ui.TEXT))
        infoBox.addView(Ui.label(this, worker.role.ifBlank { "Função não informada" }))
        top.addView(infoBox)
        top.addView(Ui.chip(this, if (worker.signatureB64.isBlank()) "SEM ASSINATURA" else "ASSINADO", if (worker.signatureB64.isBlank()) Ui.RED else Ui.GREEN))
        card.addView(top)

        if (worker.signedAt.isNotBlank()) card.addView(Ui.label(this, "Assinado em: ${worker.signedAt}").margin(0, 6.dp()))
        repo.base64ToBitmap(worker.signatureB64)?.let {
            val preview = ImageView(this)
            preview.setImageBitmap(it)
            preview.setBackgroundColor(android.graphics.Color.WHITE)
            preview.adjustViewBounds = true
            preview.maxHeight = 120.dp()
            card.addView(preview.margin(0, 8.dp()))
        }

        val actions = Ui.row(this)
        val sign = Ui.button(this, if (worker.signatureB64.isBlank()) "Assinar envolvido" else "Reassinar", if (worker.signatureB64.isBlank()) Ui.AMBER else Ui.GREEN)
        sign.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        sign.setOnClickListener {
            openSignatureDialog("Assinatura de ${worker.name}") { signature ->
                data.workers[index] = worker.copy(signatureB64 = signature, signedAt = timestamp())
                repo.save(data)
                showTeamEvidence()
            }
        }
        actions.addView(sign)
        val remove = Ui.ghostButton(this, "Remover")
        remove.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        remove.setOnClickListener {
            data.workers.removeAt(index)
            repo.save(data)
            showTeamEvidence()
        }
        actions.addView(remove)
        card.addView(actions.margin(0, 8.dp()))
        return card
    }

    private fun evidenceCard(): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.chip(this, "EVIDÊNCIAS", Ui.AMBER))
        card.addView(Ui.title(this, "Fotos do local", 18f).margin(0, 8.dp()))
        card.addView(Ui.value(this, "Fotos anexadas: ${data.photoUris.size}"))
        val button = Ui.button(this, "Anexar fotos")
        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            startActivityForResult(intent, photoRequest)
        }
        card.addView(button.margin(0, 8.dp()))
        return card
    }

    private fun signatureCard(): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.chip(this, "RESPONSÁVEL", if (data.signatureB64.isNotBlank()) Ui.GREEN else Ui.RED))
        card.addView(Ui.title(this, "Assinatura do responsável", 18f).margin(0, 8.dp()))
        val saved = data.signatureB64.isNotBlank()
        card.addView(Ui.value(this, "Status: ${if (saved) "salva" else "pendente"}", if (saved) Ui.GREEN else Ui.RED))
        repo.base64ToBitmap(data.signatureB64)?.let {
            val preview = ImageView(this)
            preview.setImageBitmap(it)
            preview.setBackgroundColor(android.graphics.Color.WHITE)
            preview.adjustViewBounds = true
            preview.maxHeight = 140.dp()
            card.addView(preview.margin(0, 8.dp()))
        }
        val button = Ui.button(this, if (saved) "Reassinar responsável" else "Assinar responsável")
        button.setOnClickListener {
            openSignatureDialog("Assinatura do responsável") { signature ->
                data.signatureB64 = signature
                repo.save(data)
                showTeamEvidence()
            }
        }
        card.addView(button.margin(0, 8.dp()))
        return card
    }

    private fun openSignatureDialog(title: String, onSaved: (String) -> Unit): Unit {
        val pad = SignaturePadView(this)
        pad.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 260.dp())
        val box = Ui.vbox(this, 12.dp())
        box.addView(pad)
        val full = Ui.button(this, "Assinar em tela cheia")
        box.addView(full.margin(0, 6.dp()))

        lateinit var dialog: AlertDialog
        dialog = AlertDialog.Builder(this)
            .setTitle(title)
            .setView(box)
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Limpar", null)
            .setPositiveButton("Salvar", null)
            .create()

        full.setOnClickListener {
            pendingSignatureSave = onSaved
            dialog.dismiss()
            val intent = Intent(this, SignatureActivity::class.java)
            intent.putExtra(SignatureActivity.EXTRA_TITLE, title)
            startActivityForResult(intent, signatureRequest)
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { pad.clearPad() }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (pad.isEmpty()) {
                    Toast.makeText(this, "Assine antes de salvar", Toast.LENGTH_SHORT).show()
                } else {
                    onSaved(repo.bitmapToBase64(pad.exportBitmap()))
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun showReview(): Unit {
        screen("Revisão e emissão", ::showPtCentral) {
            val flow = PtFlowEngine.flow(data)
            addView(documentIntro("7", "Revisar e Emitir", "Confira os dados da PT. Se houver pendências, o botão leva para a próxima seção que precisa ser corrigida.").margin(0, 8.dp()))
            addView(reviewPanel(flow).margin(0, 6.dp()))
            if (flow.critical.isNotEmpty()) {
                addView(formBlock("Pendências críticas", "Bloqueiam a emissão da PT.") {
                    flow.critical.forEach { addView(Ui.value(this@MainActivity, "• ${it.message}", Ui.RED).margin(0, 3.dp())) }
                }.margin(0, 8.dp()))
            }
            if (flow.important.isNotEmpty()) {
                addView(formBlock("Pendências importantes", "Devem ser verificadas antes da emissão.") {
                    flow.important.forEach { addView(Ui.value(this@MainActivity, "• ${it.message}", Ui.AMBER).margin(0, 3.dp())) }
                }.margin(0, 8.dp()))
            }
            val button = Ui.button(this@MainActivity, if (flow.canEmit) "Gerar e compartilhar PDF" else flow.primaryAction, if (flow.canEmit) Ui.GREEN else Ui.RED)
            button.setOnClickListener { if (flow.canEmit) generatePdf() else navigateFromFlow(flow) }
            addView(button.margin(0, 12.dp()))
        }
    }

    private fun reviewPanel(flow: PtFlowState): LinearLayout {
        val card = Ui.heroCard(this)
        val released = flow.canEmit
        card.addView(Ui.chip(this, if (released) "EMISSÃO LIBERADA" else "EMISSÃO BLOQUEADA", if (released) Ui.GREEN else Ui.RED))
        val status = Ui.title(this, if (released) "PT pronta para emissão" else "PT ainda não pode ser emitida", 22f)
        status.setTextColor(if (released) Ui.GREEN else Ui.RED)
        card.addView(status.margin(0, 8.dp()))
        card.addView(Ui.value(this, "Número: ${flow.number}"))
        card.addView(Ui.value(this, "Empresa: ${data.company.ifBlank { "-" }}"))
        card.addView(Ui.value(this, "Local: ${data.place.ifBlank { "-" }}"))
        card.addView(Ui.value(this, "Responsável: ${data.responsible.ifBlank { "-" }}"))
        card.addView(Ui.value(this, "Início: ${Ui.fmt(data.startMillis)}"))
        card.addView(Ui.value(this, "Validade: ${data.validityHours}h"))
        card.addView(Ui.value(this, "Término: ${Ui.fmt(data.endMillis)}"))
        card.addView(Ui.value(this, "Atividades: ${activitySummary()}"))
        card.addView(Ui.value(this, "Quantidade de riscos: ${RiskEngine.risksFor(data).size}"))
        val answeredChecklist = RiskEngine.checklistItems.count { data.checklist[it]?.isNotBlank() == true }
        val signedWorkers = data.workers.count { it.signatureB64.isNotBlank() }
        card.addView(Ui.value(this, "Checklist: $answeredChecklist/${RiskEngine.checklistItems.size}"))
        card.addView(Ui.value(this, "Envolvidos: ${data.workers.size}"))
        card.addView(Ui.value(this, "Assinaturas dos envolvidos: $signedWorkers/${data.workers.size}", if (signedWorkers == data.workers.size && data.workers.isNotEmpty()) Ui.GREEN else Ui.RED))
        card.addView(Ui.value(this, "Fotos: ${data.photoUris.size}"))
        val signatureSaved = data.signatureB64.isNotBlank()
        card.addView(Ui.value(this, "Assinatura do responsável: ${if (signatureSaved) "salva" else "pendente"}", if (signatureSaved) Ui.GREEN else Ui.RED))
        return card
    }

    private fun navigateFromFlow(flow: PtFlowState): Unit {
        when (flow.nextTarget) {
            PtTarget.DADOS, PtTarget.VALIDADE -> showServiceData()
            PtTarget.RISCOS -> showRisks()
            PtTarget.CHECKLIST -> showChecklist()
            PtTarget.EQUIPE -> showTeamEvidence()
            PtTarget.REVISAO -> showReview()
        }
    }

    private fun documentIntro(number: String, title: String, description: String): LinearLayout {
        val card = Ui.heroCard(this)
        val row = Ui.row(this)
        val badge = TextView(this)
        badge.text = number
        badge.gravity = Gravity.CENTER
        badge.textSize = 22f
        badge.setTextColor(Ui.AMBER_SOFT)
        badge.background = Ui.bg(Ui.PANEL, 18.dp(), Ui.AMBER, 1)
        row.addView(badge, LinearLayout.LayoutParams(58.dp(), 58.dp()))
        val box = Ui.vbox(this)
        box.setPadding(14.dp(), 0, 0, 0)
        box.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        box.addView(Ui.title(this, title, 22f))
        box.addView(Ui.label(this, description))
        row.addView(box)
        card.addView(row)
        return card
    }

    private fun formBlock(title: String, description: String, body: LinearLayout.() -> Unit): LinearLayout {
        val card = Ui.card(this)
        card.addView(Ui.section(this, title))
        if (description.isNotBlank()) card.addView(Ui.label(this, description).margin(0, 6.dp()))
        card.body()
        return card
    }

    private fun nextButton(text: String, action: () -> Unit): View {
        val button = Ui.button(this, text)
        button.setOnClickListener { saveAnd(action) }
        return button
    }

    private fun activitySummary(): String {
        val values = data.activities.toMutableList()
        if (data.manualActivity.isNotBlank()) values.add(data.manualActivity)
        return values.joinToString().ifBlank { "-" }
    }

    private fun generatePdf(): Unit {
        val flow = PtFlowEngine.flow(data)
        if (!flow.canEmit) {
            val message = flow.critical.firstOrNull()?.message ?: flow.important.firstOrNull()?.message ?: "Existem pendências para emitir a PT"
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            showReview()
            return
        }

        val file = PtPdfGenerator(this, repo).generate(data)
        val stamp = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
        data.history.add(0, PtHistoryItem(stamp, data.place, file.name))
        while (data.history.size > 10) data.history.removeAt(data.history.lastIndex)
        repo.save(data)

        val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
        val share = Intent(Intent.ACTION_SEND)
        share.type = "application/pdf"
        share.putExtra(Intent.EXTRA_STREAM, uri)
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(share, "Compartilhar PT SafeField"))
    }

    private fun timestamp(): String = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
    private fun Int.dp(): Int = Ui.dp(this@MainActivity, this)
}

class SimpleWatcher(private val after: () -> Unit) : android.text.TextWatcher {
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int): Unit = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int): Unit = Unit
    override fun afterTextChanged(s: android.text.Editable?): Unit = after()
}
