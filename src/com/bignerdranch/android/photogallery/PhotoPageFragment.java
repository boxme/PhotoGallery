package com.bignerdranch.android.photogallery;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

public class PhotoPageFragment extends VisibleFragment {
	private String mUrl;
	private WebView mWebView;
	private final String TAG = "PhotoPageFragment";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		mUrl = getActivity().getIntent().getData().toString();
	}
	
	@SuppressLint("setJavaScriptEnabled")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_photo_page, parent, false);
		
		final ProgressBar progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
		progressBar.setMax(100); 							//WebChromeClient reports in range of 0 - 100
		final TextView titleTextView = (TextView) view.findViewById(R.id.titleTextView);
		
		mWebView = (WebView) view.findViewById(R.id.webView);
		
		mWebView.getSettings().setJavaScriptEnabled(true);	//Turning JavaScript on. getSettings() get an instance of WebSettings
		
		mWebView.setWebViewClient(new WebViewClient() {							//WebViewClient is an event interface
			public boolean shouldOverrideUrlLoading(WebView view, String url) {	//Returns false to let WebView handles the URL
				return false;													//Return true is default, fires implicit intent
			}
		});
		
		mWebView.setWebChromeClient(new WebChromeClient() {			//WebChromeClient is an interface to respond to rendering events
			public void onProgressChanged(WebView webView, int progress) {	//Progress update
				if (progress == 100) {
					progressBar.setVisibility(View.INVISIBLE);
				} else {
					progressBar.setVisibility(View.VISIBLE);
					progressBar.setProgress(progress);
				}
			}
			
			public void onReceivedTitle(WebView webView, String title) {	//Title update
				titleTextView.setText(title);
			}
		});
		
		mWebView.loadUrl(mUrl);							//Loading the URL has to be done after configuring the WebView
		
		return view;
	}
}
