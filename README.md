# AndroidCodeEditor
Este es un editor de texto/código pensado para ser integrado como un componente modular de la UI general. 

El objetivo es proporcionar un potente editor que pueda ser utilizado como cualquier otra Vista. 

El editor de texto realizado con AceEditor se ha utilizado para este propósito porque es rico en características, rápido y fácil de modificar e incrustar en las aplicaciones. 

Integración con el proyecto existente 
---

### Configuraciones

##### build.gradle (project)
```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

#### build.gradle (app)
```groovy
dependencies {
    implementation 'com.github.estivensh4:AndroidCodeEditor:1.2.3'
}
```

### Uso básico
#### XML
```xml
<com.github.estivensh4.androidcodeeditor.CodeEditor
        android:id="@+id/editor"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```

#### Kotlin
Demo Activity:
```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.editor.setOnLoadedEditorListener(object : OnLoadedEditorListener {
            override fun onCreate() {
                with(binding){
                    editor.setTheme(CodeEditor.Theme.TOMORROW)
                    editor.language(CodeEditor.Language.PHP)
                    // o editor.languageString("php")
                    editor.setText("<?php\n" +
                            "\n" +
                            "?>")
                    editor.setFontSize(18)
                    editor.setSoftWrap(true)
                }
            }
        })
    }
}
```
