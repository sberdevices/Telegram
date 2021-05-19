package ru.sberdevices.sbdv.view.alphabeticrecyclerview

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.AnyThread
import androidx.annotation.MainThread
import androidx.annotation.Nullable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import org.telegram.messenger.R
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

class AlphabeticRecyclerView(context: Context, attrs: AttributeSet) :
    RecyclerView(context, attrs) {

    private var alphabet = mutableListOf<String>()

    private var fontSize = 0f
    private var itemsColor = Color.BLACK
    private var itemHorizontalPadding = 0

    private val adapter: AlphabeticAdapter = AlphabeticAdapter(context)
    private val snapHelper = LinearSnapHelper()

    private fun fetchAttributes(context: Context, attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.AlphabeticRecyclerView)

        itemHorizontalPadding =
            typedArray.getDimensionPixelSize(
                R.styleable.AlphabeticRecyclerView_itemHorizontalPadding,
                itemHorizontalPadding
            )

        val defaultSize = context.spToPx(12)
        val attFontSizeValue =
            typedArray.getDimensionPixelSize(R.styleable.AlphabeticRecyclerView_fontSize, defaultSize)
        fontSize = context.pxToSp(attFontSizeValue)

        val aItemsColor: Int = R.styleable.AlphabeticRecyclerView_itemsColor
        if (typedArray.hasValue(aItemsColor)) {
            itemsColor = context.getColor(typedArray.getResourceId(aItemsColor, 0))
        }

        typedArray.recycle()
    }

    init {
        fetchAttributes(context, attrs)
        setHasFixedSize(true)
        setAdapter(adapter)
        layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        itemAnimator = null
        overScrollMode = OVER_SCROLL_NEVER

        addItemDecoration(BorderlinePaddingCenteredItemDecorator())

        snapHelper.attachToRecyclerView(this)

        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            /* optimisation for prevent excess calling checkUpdateMiddleItem(..) on main thread */
            private val DX_THRESHOLD = context.spToPx(fontSize.roundToInt())
            private var sumDx = DX_THRESHOLD

            @AnyThread
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                recyclerView.post {
                    sumDx += dx
                    if (abs(sumDx) >= DX_THRESHOLD) {
                        sumDx = 0
                        checkUpdateMiddleItem()
                    }
                }
            }
        })
    }

    private val recyclerViewCenterX by lazy {
        val recyclerViewLocation = IntArray(2)
        this.getLocationOnScreen(recyclerViewLocation)
        val recyclerViewCenterX = recyclerViewLocation[0] + this.width / 2
        recyclerViewCenterX
    }

    @MainThread
    private fun checkUpdateMiddleItem() {
        val linearLayoutManager = layoutManager as LinearLayoutManager
        val firstVisibleIndex = linearLayoutManager.findFirstVisibleItemPosition()
        val lastVisibleIndex = linearLayoutManager.findLastVisibleItemPosition()
        /* check only views in center as optimization */
        val centerVisibleIndex = (firstVisibleIndex + lastVisibleIndex) / 2
        for (i in centerVisibleIndex - 1..lastVisibleIndex + 1) {
            val viewHolder = findViewHolderForLayoutPosition(i) as AlphabeticAdapter.ViewHolder?
            if (viewHolder?.itemView == null) {
                continue
            }
            val location = IntArray(2)
            viewHolder.itemView.getLocationOnScreen(location)

            val viewX = location[0]

            val isInMiddle = abs(recyclerViewCenterX - viewX) < viewHolder.itemView.width
            if (isInMiddle) {
                adapter.setAdapterPosition(i)
                return
            }
        }
    }

    @AnyThread
    fun setCustomAlphabet(alphabet: Set<String>) {
        this.alphabet.clear()
        this.alphabet.addAll(alphabet)
        this.alphabet.add(UNKNOWN_POSITION_SYMBOL)
        adapter.notifyDataSetChanged()
    }

    @AnyThread
    fun setAlphabet(vararg alphabets: Alphabet) {
        alphabet.clear()
        alphabets.forEach { a ->
            alphabet.addAll(a.letters)
        }
        alphabet.add(UNKNOWN_POSITION_SYMBOL)
        adapter.notifyDataSetChanged()
    }

    @AnyThread
    fun setPositionListener(@Nullable listener: Listener?) {
        adapter.setListener(listener)
    }

    @MainThread
    fun setPosition(letter: String) {
        val index = alphabet.indexOf(letter.toUpperCase())
        if (index < 0){
            setUnknownPosition()
            return
        }
        Log.d(TAG, "setPosition('$letter', $index)")
        if (index >= 0 && index != adapter.currentPosition) {
            val smoothScroller = CenterSmoothScroller(context)
            smoothScroller.targetPosition = index
            layoutManager?.startSmoothScroll(smoothScroller)
        }
    }

    @MainThread
    fun setUnknownPosition() {
        Log.d(TAG, "setUnknownPosition()")
        val index = alphabet.size - 1
        val smoothScroller = CenterSmoothScroller(context)
        smoothScroller.targetPosition = index
        layoutManager?.startSmoothScroll(smoothScroller)
    }

    internal inner class AlphabeticAdapter(context: Context?) :
        Adapter<AlphabeticAdapter.ViewHolder>() {

        var currentPosition = -1
        private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
        private var listener: Listener? = null

        fun setListener(sectionIndexClickListener: Listener?) {
            listener = sectionIndexClickListener
        }

        @Synchronized
        fun setAdapterPosition(position: Int) {
            if (currentPosition == position) return
            currentPosition = position
            val character = alphabet[position]
            listener?.onPositionChange(position, character)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view: View = layoutInflater.inflate(R.layout.alphabetic_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val letter = alphabet[position]
            holder.bind(position, letter)
        }

        override fun getItemCount(): Int {
            return alphabet.size
        }

        internal inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), OnClickListener {

            private val letterTextView: TextView = itemView.findViewById(R.id.letterTextView)
            private val normalTypeface = Typeface.defaultFromStyle(Typeface.NORMAL)
            private val boldTypeface = Typeface.defaultFromStyle(Typeface.BOLD)
            private val SCALE_DISTANCE_COEF = 0.75f

            init {
                itemView.setOnClickListener(this)
                letterTextView.run {
                    setPadding(
                        itemHorizontalPadding,
                        0,
                        itemHorizontalPadding,
                        0
                    )
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, fontSize)
                }
            }

            override fun onClick(view: View) {
                val letter = letterTextView.text.toString()
                listener?.onItemClicked(adapterPosition, letter)
                setPosition(letter)
            }

            fun bind(position: Int, letter: String) {
                letterTextView.run {
                    text = letter
                    if (position == currentPosition) {
                        typeface = boldTypeface
                        scaleX = 1f
                        scaleY = 1f
                        alpha = 1f
                    } else {
                        val distanceCoeff = SCALE_DISTANCE_COEF.pow(abs(position - currentPosition))
                        typeface = normalTypeface
                        alpha = distanceCoeff
                        scaleX = distanceCoeff
                        scaleY = distanceCoeff
                    }
                    setTextColor(itemsColor)
                }
            }
        }
    }

    override fun fling(velocityX: Int, velocityY: Int): Boolean {
        return super.fling((velocityX * FLING_SPEED_COEF).toInt(), (velocityY * FLING_SPEED_COEF).toInt())
    }

    interface Listener {
        fun onPositionChange(position: Int, character: String)
        fun onItemClicked(position: Int, character: String)
    }

    private companion object {
        const val TAG = "AlphabeticRecyclerView"
        const val FLING_SPEED_COEF = 0.5
        const val UNKNOWN_POSITION_SYMBOL = "#"
    }
}