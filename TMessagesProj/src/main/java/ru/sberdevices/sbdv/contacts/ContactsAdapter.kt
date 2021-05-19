package ru.sberdevices.sbdv.contacts

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.MainThread
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import org.telegram.messenger.R
import ru.sberdevices.sbdv.model.Contact
import ru.sberdevices.sbdv.util.isSameList
import ru.sberdevices.sbdv.view.AvatarView

private const val TAG = "ContactsAdapter"

class ContactsAdapter(
    private val itemBoundListener: (Int, Contact) -> Unit,
    private val itemClickListener: (Int) -> Unit
) : RecyclerView.Adapter<ContactHolder>() {

    private val contacts: MutableList<Contact> = mutableListOf()

    /* optimisation. cache first positions for each letters  */
    private val firstLettersFirstCardIndexMap = hashMapOf<String, Int>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.sbdv_contact, parent, false)
        view.tag = view.findViewById<TextView>(R.id.text_number)
        return ContactHolder(view)
    }

    override fun getItemCount() = contacts.size

    override fun onBindViewHolder(holder: ContactHolder, position: Int) {
        holder.bind(contacts[position], position, itemBoundListener, itemClickListener)
    }

    fun getContacts(): List<Contact> {
        return contacts
    }

    fun setContacts(sortedContacts: List<Contact>, checkEquals: Boolean = true) {
        if (checkEquals) {
            val theSameList = this.contacts.isSameList(sortedContacts)
            if (theSameList) {
                return
            }
        }
        this.contacts.clear()
        this.contacts.addAll(sortedContacts)

        var lastLetter = ""
        sortedContacts.forEachIndexed { index, contact ->
            val firstLetter = contact.firstNameLetter
            firstLetter?.let { letter ->
                if (letter != lastLetter) {
                    lastLetter = letter
                    firstLettersFirstCardIndexMap[lastLetter] = index
                }
            }
        }

        notifyDataSetChanged()
    }

    fun getContactByPosition(position: Int): Contact {
        return contacts[position]
    }

    @MainThread
    fun getFirstIndexByFirstLetter(letter: String): Int? {
        return firstLettersFirstCardIndexMap[letter]
    }

    fun updateCardNumbers(firstVisiblePosition: Int, lastVisiblePosition: Int) {
        Log.d(TAG, "updateCardNumbers() [$firstVisiblePosition, $lastVisiblePosition]")
        val updatedContacts = contacts.mapIndexed { index, contact ->
            contact
                .copy(
                    displayedNumber = when {
                        index in firstVisiblePosition..lastVisiblePosition -> index + 1 - firstVisiblePosition
                        firstVisiblePosition == RecyclerView.NO_POSITION || lastVisiblePosition == RecyclerView.NO_POSITION -> index + 1
                        else -> null
                    }
                )
                .also {
                    if (contact.displayedNumber != null) {
                        Log.d(TAG, "Contact ${contact.id} with index $index. Setting displayed number ${it.displayedNumber}")
                    }
                }
        }
        setContacts(updatedContacts)
    }

    fun clearCardNumbers() {
        setContacts(contacts.map { it.copy(displayedNumber = null) })
    }
}

class ContactHolder(private val view: View) : RecyclerView.ViewHolder(view) {

    private val avatarView = view.findViewById<AvatarView>(R.id.avatar)
    private val positionNumberView = view.findViewById<TextView>(R.id.text_number)
    private val textFullNameView = view.findViewById<TextView>(R.id.text_full_name)

    fun bind(
        contact: Contact,
        position: Int,
        itemBoundListener: (Int, Contact) -> Unit,
        itemClickListener: (Int) -> Unit
    ) {
        positionNumberView.isVisible = contact.displayedNumber != null
        positionNumberView.text = contact.displayedNumber?.toString()

        textFullNameView.text = if (contact.firstName.isNullOrEmpty()) {
            if (contact.lastName.isNullOrEmpty()) {
                contact.user.username
            } else {
                contact.lastName
            }
        } else {
            "${contact.firstName}\n${contact.lastName.orEmpty()}"
        }
        avatarView.setUser(contact.user)
        itemBoundListener.invoke(position, contact)
        view.setOnClickListener { itemClickListener.invoke(contact.id) }
    }
}
