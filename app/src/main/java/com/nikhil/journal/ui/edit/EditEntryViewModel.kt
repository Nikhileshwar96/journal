package com.nikhil.journal.ui.edit

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.journal.data.JournalRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate

/**
 * Holds the draft of a new or existing entry. Every field is mirrored into [SavedStateHandle]
 * so unsaved text survives rotation and process death.
 */
class EditEntryViewModel(
    private val repository: JournalRepository,
    private val appScope: CoroutineScope,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** Null when creating a new entry. */
    val entryId: Long? = savedStateHandle.get<Long>(ARG_ENTRY_ID)?.takeIf { it > 0 }
    val isNew: Boolean get() = entryId == null

    var title by savedString(KEY_TITLE)
        private set
    var body by savedString(KEY_BODY)
        private set

    var entryDate: LocalDate by mutableStateOf(
        LocalDate.ofEpochDay(savedStateHandle[KEY_DATE] ?: LocalDate.now().toEpochDay()),
    )
        private set

    /** Relative paths of attached images, in display order. */
    var imagePaths: List<String> by mutableStateOf(savedStateHandle.get<ArrayList<String>>(KEY_IMAGES).orEmpty())
        private set

    var isLoading by mutableStateOf(entryId != null && savedStateHandle.get<Boolean>(KEY_LOADED) != true)
        private set
    var importingCount by mutableIntStateOf(0)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
        private set
    /** Becomes true once saved; the screen then navigates away. */
    var isSaved by mutableStateOf(false)
        private set

    /** Images that were already saved with the entry before this edit session. */
    private var originalImages: List<String> = savedStateHandle.get<ArrayList<String>>(KEY_ORIGINAL_IMAGES).orEmpty()
    private var original: Snapshot = savedStateHandle.get<ArrayList<String>>(KEY_ORIGINAL)
        ?.let { Snapshot(it[0], it[1], it[2].toLong(), originalImages) }
        ?: Snapshot("", "", entryDate.toEpochDay(), emptyList())

    val hasUnsavedChanges: Boolean
        get() = current() != original

    val canSave: Boolean
        get() = !isLoading && !isSaving && importingCount == 0 && isSaved.not() &&
            (title.isNotBlank() || body.isNotBlank() || imagePaths.isNotEmpty())

    init {
        if (isLoading && entryId != null) {
            viewModelScope.launch {
                val existing = repository.getEntry(entryId)
                if (existing != null) {
                    updateTitle(existing.entry.title)
                    updateBody(existing.entry.body)
                    updateDate(existing.entry.entryDate)
                    setImages(existing.sortedImages.map { it.filePath })
                    originalImages = imagePaths
                    savedStateHandle[KEY_ORIGINAL_IMAGES] = ArrayList(originalImages)
                } else {
                    message = "This entry no longer exists."
                }
                original = current()
                saveOriginal()
                savedStateHandle[KEY_LOADED] = true
                isLoading = false
            }
        } else if (!savedStateHandle.contains(KEY_ORIGINAL)) {
            saveOriginal()
        }
    }

    fun updateTitle(value: String) {
        title = value
    }

    fun updateBody(value: String) {
        body = value
    }

    fun updateDate(date: LocalDate) {
        entryDate = date
        savedStateHandle[KEY_DATE] = date.toEpochDay()
    }

    fun removeImage(path: String) {
        setImages(imagePaths - path)
        // Files added in this session can go right away; previously saved ones are only
        // deleted when the entry is saved, so "discard" keeps the entry intact.
        if (path !in originalImages) appScope.launch { repository.images.delete(listOf(path)) }
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        importingCount += uris.size
        viewModelScope.launch {
            var failed = 0
            for (uri in uris) {
                val path = repository.images.import(uri)
                if (path != null) setImages(imagePaths + path) else failed++
                importingCount--
            }
            if (failed > 0) {
                message = if (failed == 1) "Couldn't add 1 image." else "Couldn't add $failed images."
            }
        }
    }

    /** Creates a temp file + content Uri for the camera app; remembered across process death. */
    fun prepareCameraCapture(): Uri {
        val file = repository.images.newCameraTempFile()
        savedStateHandle[KEY_CAMERA_FILE] = file.absolutePath
        return repository.images.shareableUri(file)
    }

    fun onCameraResult(success: Boolean) {
        val file = savedStateHandle.get<String>(KEY_CAMERA_FILE)?.let(::File) ?: return
        savedStateHandle.remove<String>(KEY_CAMERA_FILE)
        if (!success || !file.exists() || file.length() == 0L) {
            file.delete()
            return
        }
        importingCount++
        viewModelScope.launch {
            val path = repository.images.importCameraCapture(file, repository.images.shareableUri(file))
            if (path != null) setImages(imagePaths + path) else message = "Couldn't save the photo."
            importingCount--
        }
    }

    fun onCameraUnavailable() {
        savedStateHandle.get<String>(KEY_CAMERA_FILE)?.let { File(it).delete() }
        savedStateHandle.remove<String>(KEY_CAMERA_FILE)
        message = "No camera app available."
    }

    fun save() {
        if (!canSave) return
        isSaving = true
        viewModelScope.launch {
            try {
                repository.saveEntry(entryId, title.trim(), body.trimEnd(), entryDate, imagePaths)
                isSaved = true
            } catch (e: Exception) {
                message = "Couldn't save the entry."
            } finally {
                isSaving = false
            }
        }
    }

    fun resolve(relativePath: String): File = repository.images.resolve(relativePath)

    fun messageShown() {
        message = null
    }

    override fun onCleared() {
        // Leaving without saving: drop image files that were copied in during this session.
        if (!isSaved) {
            val orphans = imagePaths - originalImages.toSet()
            if (orphans.isNotEmpty()) appScope.launch { repository.images.delete(orphans) }
        }
    }

    private fun setImages(paths: List<String>) {
        imagePaths = paths
        savedStateHandle[KEY_IMAGES] = ArrayList(paths)
    }

    private fun current() = Snapshot(title, body, entryDate.toEpochDay(), imagePaths)

    private fun saveOriginal() {
        savedStateHandle[KEY_ORIGINAL] =
            arrayListOf(original.title, original.body, original.epochDay.toString())
    }

    private data class Snapshot(
        val title: String,
        val body: String,
        val epochDay: Long,
        val images: List<String>,
    )

    private fun savedString(key: String) = object : kotlin.properties.ReadWriteProperty<Any?, String> {
        private var state by mutableStateOf(savedStateHandle.get<String>(key) ?: "")
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = state
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: String) {
            state = value
            savedStateHandle[key] = value
        }
    }

    companion object {
        const val ARG_ENTRY_ID = "entryId"
        private const val KEY_TITLE = "title"
        private const val KEY_BODY = "body"
        private const val KEY_DATE = "date"
        private const val KEY_IMAGES = "images"
        private const val KEY_ORIGINAL = "original"
        private const val KEY_ORIGINAL_IMAGES = "originalImages"
        private const val KEY_LOADED = "loaded"
        private const val KEY_CAMERA_FILE = "cameraFile"
    }
}
