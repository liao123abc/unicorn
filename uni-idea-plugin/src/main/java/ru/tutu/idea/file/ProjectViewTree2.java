// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package ru.tutu.idea.file;

import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.impl.ProjectViewRenderer;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDirectoryContainer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.DirtyUI;
import com.intellij.ui.popup.HintUpdateSupply;
import com.intellij.ui.tabs.FileColorManagerImpl;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class ProjectViewTree2 extends DnDAwareTree {
  private static final Logger LOG = Logger.getInstance(ProjectViewTree2.class);

  public ProjectViewTree2(TreeModel model) {
    super((TreeModel)null);
    setLargeModel(true);
    setModel(model);
    setCellRenderer(createCellRenderer());
    HintUpdateSupply.installDataContextHintUpdateSupply(this);
  }

  /**
   * @return custom renderer for tree nodes
   */
  @NotNull
  protected TreeCellRenderer createCellRenderer() {
    return new ProjectViewRenderer();
  }

  /**
   * @deprecated Not every tree employs {@link DefaultMutableTreeNode} so
   * use {@link #getSelectionPaths()} or {@link TreeUtil#getSelectedPathIfOne(JTree)} directly.
   */
  @Deprecated
  public DefaultMutableTreeNode getSelectedNode() {
    TreePath path = TreeUtil.getSelectedPathIfOne(this);
    return path == null ? null : ObjectUtils.tryCast(path.getLastPathComponent(), DefaultMutableTreeNode.class);
  }

  @Override
  public void setToggleClickCount(int count) {
    if (count != 2) LOG.info(new IllegalStateException("setToggleClickCount: unexpected count = " + count));
    super.setToggleClickCount(count);
  }

  @Override
  public boolean isFileColorsEnabled() {
    return ProjectViewTreeHelpers.isFileColorsEnabledFor(this);
  }

  @DirtyUI
  @Nullable
  @Override
  public Color getFileColorFor(Object obj) {
    if (obj instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)obj;
      obj = node.getUserObject();
    }
    if (obj instanceof AbstractTreeNode) {
      AbstractTreeNode<?> node = (AbstractTreeNode<?>)obj;
      Object value = node.getValue();
      if (value instanceof PsiElement) {
        return ProjectViewTreeHelpers.getColorForElement((PsiElement)value);
      }
    }
    if (obj instanceof ProjectViewNode) {
      ProjectViewNode<?> node = (ProjectViewNode<?>)obj;
      VirtualFile file = node.getVirtualFile();
      if (file != null) {
        Project project = node.getProject();
        if (project != null && !project.isDisposed()) {
          Color color = VfsPresentationUtil.getFileBackgroundColor(project, file);
          if (color != null) return color;
        }
      }
    }
    return null;
  }

}
