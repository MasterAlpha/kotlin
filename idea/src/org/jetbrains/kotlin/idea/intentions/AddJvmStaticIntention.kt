/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.idea.runSynchronouslyWithProgress
import org.jetbrains.kotlin.idea.search.restrictByFileType
import org.jetbrains.kotlin.idea.util.addAnnotation
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.findAnnotation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject

class AddJvmStaticIntention : SelfTargetingIntention<KtNamedDeclaration>(
        KtNamedDeclaration::class.java,
        "Add '@JvmStatic' annotation"
), LowPriorityAction {
    private val JvmStaticFqName = FqName("kotlin.jvm.JvmStatic")
    private val JvmFieldFqName = FqName("kotlin.jvm.JvmField")

    override fun startInWriteAction() = false

    override fun isApplicableTo(element: KtNamedDeclaration, caretOffset: Int): Boolean {
        if (element !is KtNamedFunction && element !is KtProperty) return false
        val containingObject = element.containingClassOrObject as? KtObjectDeclaration ?: return false
        if (containingObject.isObjectLiteral()) return false
        if (element is KtProperty) {
            if (element.hasModifier(KtTokens.CONST_KEYWORD)) return false
            if (element.findAnnotation(JvmFieldFqName) != null) return false
        }
        return element.findAnnotation(JvmStaticFqName) == null
    }

    override fun applyTo(element: KtNamedDeclaration, editor: Editor?) {
        val containingObject = element.containingClassOrObject as? KtObjectDeclaration ?: return
        val isCompanionMember = containingObject.isCompanion()
        val instanceFieldName = if (isCompanionMember) containingObject.name else JvmAbi.INSTANCE_FIELD
        val instanceFieldContainingClass = if (isCompanionMember) containingObject.containingClassOrObject ?: return else containingObject
        val project = element.project

        val expressionsToReplaceWithQualifier = project.runSynchronouslyWithProgress("Looking for usages in Java files...", true) {
            runReadAction {
                val searchScope = element.useScope.restrictByFileType(JavaFileType.INSTANCE)
                ReferencesSearch
                        .search(element, searchScope)
                        .mapNotNull {
                            val refExpr = it.element as? PsiReferenceExpression ?: return@mapNotNull null
                            if ((refExpr.resolve() as? KtLightElement<*, *>)?.kotlinOrigin != element) return@mapNotNull null
                            val qualifierExpr = refExpr.qualifierExpression as? PsiReferenceExpression ?: return@mapNotNull null
                            if (qualifierExpr.qualifierExpression == null) return@mapNotNull null
                            val instanceField = qualifierExpr.resolve() as? KtLightField ?: return@mapNotNull null
                            if (instanceField.name != instanceFieldName) return@mapNotNull null
                            if ((instanceField.containingClass as? KtLightClass)?.kotlinOrigin != instanceFieldContainingClass) return@mapNotNull null
                            qualifierExpr
                        }
            }
        } ?: return

        runWriteAction {
            element.addAnnotation(JvmStaticFqName)
            expressionsToReplaceWithQualifier.forEach { it.replace(it.qualifierExpression!!) }
        }
    }
}
