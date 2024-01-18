package indexation

import model.ImportInfo
import component.KotlinEnvironment
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.jvm.plugins.PluginCliParser
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.ir.backend.js.prepareAnalyzedSourceModule
import org.jetbrains.kotlin.library.impl.isKotlinLibrary
import java.io.File

class WebIndexationBuilder(
  private val kotlinEnvironment: KotlinEnvironment,
  private val configuration: CompilerConfiguration,
  private val libraries: List<String>,
  private val compilerPlugins: Boolean
): IndexationBuilder() {

  override fun getAllIndexes(): List<ImportInfo> =
    kotlinEnvironment.environment { coreEnvironment ->
      val project = coreEnvironment.project

      if (compilerPlugins) {
        PluginCliParser.loadPluginsSafe(
          kotlinEnvironment.COMPILER_PLUGINS,
          kotlinEnvironment.compilerPluginOptions,
          emptyList(),
          configuration
        )
      }
      val sourceModule = prepareAnalyzedSourceModule(
        project,
        coreEnvironment.getSourceFiles(),
        configuration,
        libraries.filter { isKotlinLibrary(File(it)) },
        friendDependencies = emptyList(),
        analyzer = AnalyzerWithCompilerReport(kotlinEnvironment.jsConfiguration),
      )

      val mds = sourceModule.allDependencies.map {
        sourceModule.getModuleDescriptor(it)
      }

      return@environment mds.flatMap { moduleDescriptor ->
        moduleDescriptor.allImportsInfo()
      }.distinct()
    }
}