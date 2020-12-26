// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Suppress("UnstableApiUsage")

package com.intellij.ide.projectView.impl

import com.intellij.ide.PsiCopyPasteManager
import com.intellij.ide.projectView.BaseProjectTreeBuilder
import com.intellij.ide.projectView.ProjectViewSettings
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.ui.customization.CustomizationUtil
import com.intellij.ide.util.treeView.AbstractTreeBuilder
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.ide.util.treeView.AbstractTreeStructure
import com.intellij.ide.util.treeView.AbstractTreeUpdater
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.util.EditSourceOnDoubleClickHandler
import com.intellij.util.EditSourceOnEnterKeyHandler
import com.intellij.util.ui.tree.TreeUtil
import ru.tutu.idea.file.FILES_PANE_ID
import ru.tutu.idea.file.uniFilesRootNodes
import java.awt.Font
import javax.swing.JComponent
import javax.swing.ToolTipManager
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

class ProjectViewPSIPane2 constructor(project: Project) : AbstractProjectViewPane2(project) {

  override fun getTitle(): String = "todo title pane"
  override fun getId(): String = FILES_PANE_ID

  fun createComponent(rootPaths: List<VirtualFile>): JComponent {
    val treeModel = DefaultTreeModel(DefaultMutableTreeNode(null))
    val tree: ProjectViewTree =
      object : ProjectViewTree(treeModel) {
        override fun toString(): String = "todo title" + " " + super.toString()//todo title
        override fun setFont(font: Font) {
          super.setFont(font.deriveFont(font.size /*+ 3f*/))
        }
      }
    val treeStructure: ProjectAbstractTreeStructureBase =
      object : ProjectTreeStructure(myProject, FILES_PANE_ID), ProjectViewSettings {
        override fun createRoot(project: Project, settings: ViewSettings): AbstractTreeNode<*> =
          object : ProjectViewProjectNode(project, settings) {
            override fun canRepresent(element: Any): Boolean = true
            override fun getChildren(): Collection<AbstractTreeNode<*>> {
              return uniFilesRootNodes(project, settings, rootDirs = rootPaths)
            }
          }

        override fun getChildElements(element: Any): Array<Any> {
          val treeNode = element as AbstractTreeNode<*>
          val elements = treeNode.children
          elements.forEach { it.setParent(treeNode) }
          return elements.toTypedArray()
        }

        override fun isShowExcludedFiles(): Boolean = true
        override fun isShowLibraryContents(): Boolean = true
        override fun isUseFileNestingRules(): Boolean = true
      }

    myTree = tree
    enableDnD()
    myTreeStructure = treeStructure
    val treeBuilder: BaseProjectTreeBuilder =
      object : ProjectTreeBuilder(
        myProject,
        myTree,
        treeModel,
        null,
        treeStructure
      ) {
        override fun createUpdater() = createTreeUpdater(this, treeStructure)
      }
    installComparator(treeBuilder)
    setTreeBuilder(treeBuilder)
    initTree()
    return ScrollPaneFactory.createScrollPane(myTree)
  }

  private fun initTree() {
    myTree.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
    myTree.isRootVisible = false
    myTree.showsRootHandles = true
    myTree.expandPath(TreePath(myTree.model.root))
    EditSourceOnDoubleClickHandler.install(myTree)
    EditSourceOnEnterKeyHandler.install(myTree)
    ToolTipManager.sharedInstance().registerComponent(myTree)
    TreeUtil.installActions(myTree)
    SpeedSearchFiles(myTree)
    myTree.addKeyListener(PsiCopyPasteManager.EscapeHandler())
    CustomizationUtil.installPopupHandler(
      myTree,
      IdeActions.GROUP_PROJECT_VIEW_POPUP,
      ActionPlaces.PROJECT_VIEW_POPUP
    )
  }

}

fun createTreeUpdater(treeBuilder: AbstractTreeBuilder, treeStructure: AbstractTreeStructure): AbstractTreeUpdater =
  object : AbstractTreeUpdater(treeBuilder) {
    override fun addSubtreeToUpdateByElement(element: Any): Boolean {
      if (element is PsiDirectory) {
        var dirToUpdateFrom: PsiDirectory? = element

        var addedOk: Boolean
        while (!super.addSubtreeToUpdateByElement(dirToUpdateFrom ?: treeStructure.rootElement)
            .also { addedOk = it }
        ) {
          if (dirToUpdateFrom == null) {
            break
          }
          dirToUpdateFrom = dirToUpdateFrom.parentDirectory
        }
        return addedOk
      }
      return super.addSubtreeToUpdateByElement(element)
    }
  }
