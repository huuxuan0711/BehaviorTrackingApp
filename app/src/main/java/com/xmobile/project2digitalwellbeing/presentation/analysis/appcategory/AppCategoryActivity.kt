package com.xmobile.project2digitalwellbeing.presentation.analysis.appcategory

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.xmobile.project2digitalwellbeing.R
import com.xmobile.project2digitalwellbeing.databinding.ActivityAppCategoryBinding
import com.xmobile.project2digitalwellbeing.presentation.analysis.usagedetailapp.UsageDetailAppActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AppCategoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppCategoryBinding
    private val viewModel: AppCategoryViewModel by viewModels()
    private val categoryAdapter by lazy {
        CategoryAdapter(
            onCategoryHeaderClick = { viewModel.toggleCategoryExpansion(it.category) },
            onAppCategoryClick = { app, anchorView, imgArrow ->
                AppCategoryPopup(
                    context = this,
                    anchorView = anchorView,
                    imgArrow = imgArrow,
                    selectedCategory = app.category,
                    onCategorySelected = { category ->
                        viewModel.updateAppCategory(app.packageName, category)
                    }
                ).show() 
            },
            onAppItemClick = { app ->
                val intent = Intent(this, UsageDetailAppActivity::class.java).apply {
                    putExtra("PACKAGE_NAME", app.packageName)
                }
                startActivity(intent)
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAppCategoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupViews()
        observeUi()
    }

    private fun setupViews() {
        binding.btnBack.setOnClickListener { finish() }
        
        binding.rvCategory.apply {
            layoutManager = LinearLayoutManager(this@AppCategoryActivity)
            adapter = categoryAdapter
        }

        binding.edtSearch.addTextChangedListener {
            viewModel.onSearchQueryChanged(it?.toString() ?: "")
        }
    }

    private fun observeUi() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
                    binding.rvCategory.visibility = if (state.isLoading) View.GONE else View.VISIBLE
                    
                    categoryAdapter.submitList(state.categories)
                    
                    binding.tvEmpty.visibility = if (!state.isLoading && state.categories.isEmpty()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }
        }
    }
}
