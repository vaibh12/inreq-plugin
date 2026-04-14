package com.inreq.plugin.actions

import com.inreq.plugin.toolwindow.InReqToolWindowFactory
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.wm.ToolWindowManager

class SendRequestAction : AnAction(), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ToolWindowManager.getInstance(project).getToolWindow("InReq")?.show()
        InReqToolWindowFactory.getPanel(project)?.sendRequest()
    }
    override fun update(e: AnActionEvent) { e.presentation.isEnabledAndVisible = e.project != null }
}
