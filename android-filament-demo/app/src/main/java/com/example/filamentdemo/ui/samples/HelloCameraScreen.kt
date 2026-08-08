package com.example.filamentdemo.ui.samples

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

@SuppressLint("ClickableViewAccessibility")
@Composable
fun HelloCameraScreen() {
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember { CameraRenderer() }

    DisposableEffect(Unit) {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                renderer.startFrameCallback()
            }

            override fun onPause(owner: LifecycleOwner) {
                renderer.stopFrameCallback()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            renderer.destroy()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    // Zoom
                    renderer.onScroll(detector.focusX, detector.focusY, detector.scaleFactor - 1.0f)
                    return true
                }
            })

            val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                    // Orbit - ignore if pinch is in progress
                    if (!scaleDetector.isInProgress) {
                        renderer.onGrabUpdate(e2.x, e2.y)
                    }
                    return true
                }

                override fun onDown(e: MotionEvent): Boolean {
                    renderer.onGrabBegin(e.x, e.y)
                    return true
                }
            })

            object : SurfaceView(context) {
                override fun onTouchEvent(event: MotionEvent): Boolean {
                    // Pass to both detectors
                    scaleDetector.onTouchEvent(event)
                    gestureDetector.onTouchEvent(event)

                    // Explicit grab cleanup on release
                    if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
                        renderer.onGrabEnd()
                    }
                    return true
                }
            }.apply {
                holder.addCallback(object : SurfaceHolder.Callback {
                    override fun surfaceCreated(holder: SurfaceHolder) {
                        renderer.onSurfaceCreated(holder)
                    }

                    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                        renderer.onSurfaceChanged(holder, width, height)
                    }

                    override fun surfaceDestroyed(holder: SurfaceHolder) {
                        renderer.onSurfaceDestroyed()
                    }
                })
            }
        }
    )
}
