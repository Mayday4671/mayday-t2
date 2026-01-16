<template>
  <div class="home-container" ref="container">
    <!-- 3D Background Canvas -->
    <div ref="canvasContainer" class="canvas-container"></div>

    <!-- Navigation Menu -->
    <nav class="top-nav">
      <div class="logo">Mayday</div>
      <div class="menu-items">
        <a href="#products">产品</a>
        <a href="#solutions">解决方案</a>
        <a href="#docs">开发者文档</a>
        <a href="#about">关于我们</a>
        <a-button type="primary" shape="round" class="login-btn">登录 / 注册</a-button>
      </div>
    </nav>

    <!-- Hero Section -->
    <section class="hero-section">
       <!-- ... -->
    </section>

    <!-- Features Section (Products) -->
    <section id="products" class="section features-section">
      <div class="section-title" ref="featureTitle">
        <h2>核心能力</h2>
      </div>
      <div class="features-grid">
        <div class="feature-card" v-for="(feat, index) in features" :key="index" ref="featureCards">
          <div class="icon">{{ feat.icon }}</div>
          <h3>{{ feat.title }}</h3>
          <p>{{ feat.desc }}</p>
        </div>
      </div>
    </section>

    <!-- Content Section (Solutions) -->
    <section id="solutions" class="section content-section">
      <div class="text-block" ref="textBlock">
        <h2>无缝集成</h2>
        <p>
          在任何地方部署您的智能体。我们的平台支持所有主流云服务商和本地环境，实现真正的全球互联。
        </p>
      </div>
    </section>

    <!-- Developer Docs -->
    <section id="docs" class="section docs-section">
      <div class="section-title">
        <h2>开发者优先</h2>
      </div>
      <div class="code-preview">
        <div class="code-window">
          <div class="window-header">
            <span class="dot red"></span>
            <span class="dot yellow"></span>
            <span class="dot green"></span>
            <span class="title">agent_deploy.sh</span>
          </div>
          <pre><code>
<span class="keyword">import</span> { Mayday } <span class="keyword">from</span> '@mayday/sdk';

<span class="comment">// 初始化智能体</span>
<span class="keyword">const</span> agent = <span class="keyword">new</span> Mayday.Agent({
  id: <span class="string">'agent-007'</span>,
  model: <span class="string">'mayday-gl-4'</span>,
  capabilities: [<span class="string">'reasoning'</span>, <span class="string">'vision'</span>]
});

<span class="comment">// 部署到全球边缘网络</span>
<span class="keyword">await</span> agent.deploy({
  region: <span class="string">'global'</span>,
  latency: <span class="string">'low'</span>
});
          </code></pre>
        </div>
        <div class="docs-text">
          <h3>只需几行代码</h3>
          <p>Mayday SDK 旨在简化复杂的多智能体编排。通过直观的 API，您可以专注于业务逻辑，而非基础设施。</p>
          <a-button type="ghost" shape="round" size="large">查看 API 文档</a-button>
        </div>
      </div>
    </section>

    <!-- About Us -->
    <section id="about" class="section about-section">
      <div class="about-content">
        <h2>关于 Mayday</h2>
        <p class="mission">
          我们的使命是构建人类水平的协作智能体网络，释放数字生产力的无限潜能。
        </p>
        <div class="stats-grid">
          <div class="stat">
            <span class="number">150+</span>
            <span class="label">全球节点</span>
          </div>
          <div class="stat">
            <span class="number">10B+</span>
            <span class="label">日均推理</span>
          </div>
          <div class="stat">
            <span class="number">99.99%</span>
            <span class="label">SLA</span>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import * as THREE from 'three';
import gsap from 'gsap';
import { ScrollTrigger } from 'gsap/ScrollTrigger';

gsap.registerPlugin(ScrollTrigger);

// Refs
const canvasContainer = ref<HTMLElement | null>(null);
const heroTitle = ref<HTMLElement | null>(null);
const heroSubtitle = ref<HTMLElement | null>(null);
const heroCta = ref<HTMLElement | null>(null);
const featureTitle = ref<HTMLElement | null>(null);
const featureCards = ref<HTMLElement[]>([]);
const textBlock = ref<HTMLElement | null>(null);

// Data
const features = [
  { icon: '🧠', title: '全球认知', desc: '具备复杂问题解决能力的分布式智能推理网络。' },
  { icon: '⚡', title: '实时响应', desc: '亚毫秒级延迟，确保关键决策的即时执行。' },
  { icon: '🔒', title: '银行级安全', desc: '企业级加密与访问控制，保障数据资产安全。' },
  { icon: '🌐', title: '行星网络', desc: '跨越15+区域的分布式智能体协同系统。' },
];

// Three.js Variables
let scene: THREE.Scene;
let camera: THREE.PerspectiveCamera;
let renderer: THREE.WebGLRenderer;
let particleSystem: THREE.Points;
let starSystem: THREE.Points; 
let satelliteSystem: THREE.Object3D[] = []; // Satellites
let animationId: number;
let mouseX = 0;
let mouseY = 0;

// Event Handlers
const onDocumentMouseMove = (event: MouseEvent) => {
  mouseX = (event.clientX - window.innerWidth / 2) * 0.5;
  mouseY = (event.clientY - window.innerHeight / 2) * 0.5;
};

const onWindowResize = () => {
  if (camera && renderer) {
    camera.aspect = window.innerWidth / window.innerHeight;
    camera.updateProjectionMatrix();
    renderer.setSize(window.innerWidth, window.innerHeight);
  }
};



const initThreeJS = async () => {
  if (!canvasContainer.value) return;

  // Scene setup
  scene = new THREE.Scene();
  scene.fog = new THREE.FogExp2(0x000000, 0.001);

  // Camera settings for Globe View
  camera = new THREE.PerspectiveCamera(60, window.innerWidth / window.innerHeight, 0.1, 4000);
  camera.position.z = 450; 
  camera.position.y = 100; // Look down slightly
  camera.lookAt(0, 0, 0);

  // Renderer
  renderer = new THREE.WebGLRenderer({ alpha: true, antialias: true });
  renderer.setPixelRatio(window.devicePixelRatio);
  renderer.setSize(window.innerWidth, window.innerHeight);
  canvasContainer.value.appendChild(renderer.domElement);

  // --- Smooth Glow Texture ---
  const getTexture = () => {
    const canvas = document.createElement('canvas');
    canvas.width = 32; canvas.height = 32;
    const ctx = canvas.getContext('2d');
    if (ctx) {
      const gradient = ctx.createRadialGradient(16, 16, 0, 16, 16, 16);
      gradient.addColorStop(0, 'rgba(255, 255, 255, 1)');
      gradient.addColorStop(0.2, 'rgba(0, 150, 255, 0.6)');
      gradient.addColorStop(1, 'rgba(0, 0, 0, 0)');
      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, 32, 32);
    }
    const texture = new THREE.Texture(canvas);
    texture.needsUpdate = true;
    return texture;
  };
  const texture = getTexture();

  // --- Load Map ---
  const image = new Image();
  image.src = '/world_map.png'; 
  await new Promise<void>((resolve) => { image.onload = () => resolve(); });

  const canvas = document.createElement('canvas');
  const ctx = canvas.getContext('2d');
  if (!ctx) return;

  const w = 400; 
  const h = 200; // Equirectangular aspect ratio roughly 2:1
  canvas.width = w;
  canvas.height = h;
  ctx.drawImage(image, 0, 0, w, h);
  const data = ctx.getImageData(0, 0, w, h).data;

  // --- Globe Generation ---
  const positions: number[] = [];
  const colors: number[] = [];
  const initPositions: number[] = []; 
  const color = new THREE.Color();
  const GLOBE_RADIUS = 180;

  // Step 1 or 2 for density.
  const step = 2; 

  for (let y = 0; y < h; y += step) {
    for (let x = 0; x < w; x += step) {
      const idx = (y * w + x) * 4;
      const r = data[idx] ?? 0; // Red channel
      
      // Map is white land, black ocean.
      // If pixel is dark, skip (Ocean)
      if (r < 50) continue; 

      // Spherical Mapping
      // X maps to Longitude (-180 to 180)
      // Y maps to Latitude (90 to -90)
      const lat = (1 - y / h) * 180 - 90;
      const lon = (x / w) * 360 - 180;

      // Convert to Radians
      const phi = (90 - lat) * (Math.PI / 180);
      const theta = (lon + 180) * (Math.PI / 180);

      // Spherical to Cartesian
      // z is up in math usually, but here Y is up
      const vx = -(GLOBE_RADIUS * Math.sin(phi) * Math.cos(theta));
      const vy = GLOBE_RADIUS * Math.cos(phi);
      const vz = GLOBE_RADIUS * Math.sin(phi) * Math.sin(theta);

      positions.push(vx, vy, vz);
      initPositions.push(vx, vy, vz);

      // Color: Brighter Electric Cyan for Land
      color.setHex(0x22ccff); 
      // Slight variation based on lat/lon for depth
      color.offsetHSL(0.05, 0, (Math.random()-0.5)*0.2); 
      colors.push(color.r, color.g, color.b);
    }
  }

  // --- Atmosphere / Core Dots ---
  // Add some random particles inside and outside for volume
  for (let i = 0; i < 4000; i++) {
     const theta = Math.random() * Math.PI * 2;
     const phi = Math.acos((Math.random() * 2) - 1);
     // Random radius: some surface, some halo
     const r = GLOBE_RADIUS + (Math.random() * 50 - 10); 
     
     const x = r * Math.sin(phi) * Math.cos(theta);
     const z = r * Math.sin(phi) * Math.sin(theta);
     const y = r * Math.cos(phi);

     positions.push(x, y, z);
     initPositions.push(x, y, z);
     
     // Color: Brighter Atmosphere
     color.setHex(0x0088ff);
     colors.push(color.r, color.g, color.b);
  }

  const geometry = new THREE.BufferGeometry();
  geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3));
  geometry.setAttribute('initialPosition', new THREE.Float32BufferAttribute(initPositions, 3));
  geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3));

  const material = new THREE.PointsMaterial({
    size: 2.8, // Increased size
    map: texture,
    vertexColors: true,
    sizeAttenuation: true, // Keep attenuation but make base larger
    transparent: true,
    opacity: 1.0, // Max opacity
    depthWrite: false, 
    blending: THREE.AdditiveBlending 
  });

  particleSystem = new THREE.Points(geometry, material);
  scene.add(particleSystem);

  // --- Satellites ---
  satelliteSystem = [];
  const satMat = new THREE.MeshBasicMaterial({ color: 0xffaa00 });
  const satGeo = new THREE.SphereGeometry(2, 8, 8);
  
  for(let i=0; i<3; i++) {
      const sat = new THREE.Mesh(satGeo, satMat);
      
      // Store orbit data
      (sat as any).userData = {
          radius: GLOBE_RADIUS + 40 + i * 30, 
          speed: 0.005 + i * 0.002,
          angle: i * (Math.PI * 2 / 3), 
          axis: new THREE.Vector3(Math.random(), Math.random(), Math.random()).normalize()
      };
      
      const glow = new THREE.Sprite(new THREE.SpriteMaterial({ 
          map: texture, color: 0xff5500, transparent: true, blending: THREE.AdditiveBlending 
      }));
      glow.scale.set(20, 20, 1);
      sat.add(glow);

      satelliteSystem.push(sat);
      scene.add(sat);
  }
  
  // Clean Starfield (Background)
  const starGeo = new THREE.BufferGeometry();
  const starPos = [];
  for(let i=0; i<800; i++) {
      starPos.push((Math.random()-0.5)*3000, (Math.random()-0.5)*3000, (Math.random()-0.5)*3000);
  }
  starGeo.setAttribute('position', new THREE.Float32BufferAttribute(starPos, 3));
  starSystem = new THREE.Points(starGeo, new THREE.PointsMaterial({
      color: 0xffffff, size: 1.5, transparent: true, opacity: 0.3
  }));
  scene.add(starSystem);

  document.addEventListener('mousemove', onDocumentMouseMove);
  window.addEventListener('resize', onWindowResize);
};

// Animation Loop
const animate = () => {
  animationId = requestAnimationFrame(animate);

  // Gentle Camera Sway
  if (camera) {
    camera.position.x += (mouseX * 0.1 - camera.position.x) * 0.05;
    camera.position.y += (-mouseY * 0.1 - camera.position.y) * 0.05;
    camera.lookAt(0, 0, 0);
  }

  // Background Stars Twist
  if (starSystem) {
    starSystem.rotation.x += 0.0001;
    starSystem.rotation.y += 0.0002;
  }

  // Globe Rotation - SLOWER
  if (particleSystem) {
    // Base rotation + mouse influence
    particleSystem.rotation.y += 0.0005 + (mouseX * 0.00005); 
    particleSystem.rotation.x = mouseY * 0.0002; // Slight tilt
  }

  // Satellite Animation
  satelliteSystem.forEach((sat) => {
      const data = (sat as any).userData;
      data.angle += 0.01; 
      
      // Orbiting
      const pos = new THREE.Vector3(data.radius, 0, 0);
      pos.applyAxisAngle(data.axis, data.angle);
      sat.position.copy(pos);
  });

  if (renderer && scene && camera) {
    renderer.render(scene, camera);
  }
};

const initAnimations = () => {
  // Hero Animations
  const tl = gsap.timeline({ defaults: { ease: 'power3.out', duration: 1 } });
  
  if (heroTitle.value) tl.from(heroTitle.value, { y: 100, opacity: 0, duration: 1.2 });
  if (heroSubtitle.value) tl.from(heroSubtitle.value, { y: 50, opacity: 0 }, '-=0.8');
  if (heroCta.value) tl.from(heroCta.value, { y: 30, opacity: 0 }, '-=0.6');

  // Scroll Animations for Features
  if (featureTitle.value) {
    gsap.from(featureTitle.value, {
      scrollTrigger: {
        trigger: featureTitle.value,
        start: 'top 80%',
      },
      y: 50,
      opacity: 0,
      duration: 1
    });
  }

  if (featureCards.value.length) {
    gsap.from(featureCards.value, {
      scrollTrigger: {
        trigger: '.features-grid',
        start: 'top 75%',
      },
      y: 100,
      opacity: 0,
      stagger: 0.2,
      duration: 0.8,
      ease: 'back.out(1.7)'
    });
  }

  // Text Block Parallax
  if (textBlock.value) {
     gsap.from(textBlock.value, {
      scrollTrigger: {
        trigger: textBlock.value,
        start: 'top 80%',
        scrub: 1 // Link animation to scroll position
      },
      y: 100,
      opacity: 0.5,
    });
  }
};

onMounted(() => {
  initThreeJS();
  animate();
  initAnimations();
});

onUnmounted(() => {
  cancelAnimationFrame(animationId);
  if (renderer) {
    renderer.dispose();
  }
  document.removeEventListener('mousemove', onDocumentMouseMove);
  window.removeEventListener('resize', onWindowResize);
  // Clean up GSAP triggers
  ScrollTrigger.getAll().forEach(t => t.kill());
});
</script>

<style scoped>
.home-container {
  position: relative;
  width: 100%;
  background-color: #000;
  color: #fff;
  overflow-x: hidden;
}

.canvas-container {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100vh;
  z-index: 0; /* Behind content */
  pointer-events: none;
}

/* Navigation */
.top-nav {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  padding: 20px 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 1000;
  pointer-events: auto;
}

.top-nav .logo {
  font-size: 1.5rem;
  font-weight: 700;
  letter-spacing: 1px;
}

.top-nav .menu-items {
  display: flex;
  gap: 30px;
  align-items: center;
}

.top-nav a {
  color: rgba(255, 255, 255, 0.8);
  text-decoration: none;
  font-size: 0.95rem;
  transition: color 0.3s;
}

.top-nav a:hover {
  color: #fff;
}

.top-nav .login-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
}

.top-nav .login-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  border-color: #fff;
}

/* Sections */
.section {
  position: relative;
  z-index: 1; /* Above canvas */
  padding: 100px 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.hero-section {
  position: relative;
  z-index: 1;
  height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  padding: 0 20px;
}

.hero-title {
  font-size: 5rem;
  font-weight: 800;
  line-height: 1.1;
  margin-bottom: 24px;
  letter-spacing: -2px;
}

.highlight {
  background: linear-gradient(90deg, #00c6fb, #005bea);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

.hero-subtitle {
  font-size: 1.5rem;
  color: #aaa;
  max-width: 600px;
  margin-bottom: 40px;
}

.cta-group {
  display: flex;
  gap: 20px;
}

.cta-btn {
  height: 50px;
  padding: 0 40px;
  font-size: 16px;
  font-weight: 600;
}

.cta-btn.primary {
  background: linear-gradient(90deg, #005bea 0%, #00c6fb 100%);
  border: none;
}

.cta-btn.ghost {
  background: transparent;
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
}

.cta-btn.ghost:hover {
  border-color: #fff;
  color: #fff;
}

/* Features */
.features-section {
  min-height: 80vh;
}

.section-title h2 {
  font-size: 3rem;
  text-align: center;
  margin-bottom: 60px;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 30px;
}

.feature-card {
  background: rgba(255, 255, 255, 0.05);
  backdrop-filter: blur(10px);
  padding: 40px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: transform 0.3s;
}

.feature-card:hover {
  transform: translateY(-10px);
  background: rgba(255, 255, 255, 0.08);
}

.feature-card .icon {
  font-size: 3rem;
  margin-bottom: 20px;
}

.feature-card h3 {
  font-size: 1.5rem;
  margin-bottom: 15px;
  color: #fff;
}

.feature-card p {
  color: #ccc;
  line-height: 1.6;
}

/* Content Section */
.content-section {
  min-height: 80vh;
  display: flex;
  align-items: center;
  justify-content: center;
}

.text-block {
  text-align: center;
  max-width: 800px;
}

.text-block h2 {
  font-size: 4rem;
  background: linear-gradient(135deg, #fff 0%, #888 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 30px;
}

.text-block p {
  font-size: 1.8rem;
  color: #888;
}

/* Responsive */
@media (max-width: 768px) {
  .hero-title {
    font-size: 3.5rem;
  }
  
  .text-block h2 {
    font-size: 2.5rem;
  }
  
/* ... existing responsive styles ... */
  .cta-group {
    flex-direction: column;
  }
}

/* Developer Docs */
.docs-section {
  min-height: 80vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: linear-gradient(180deg, transparent 0%, rgba(0,0,0,0.8) 100%);
}

.code-preview {
  display: flex;
  gap: 60px;
  align-items: center;
  max-width: 1200px;
  width: 100%;
  padding: 40px;
}

.code-window {
  background: #1e1e1e;
  border-radius: 12px;
  box-shadow: 0 20px 50px rgba(0,0,0,0.5);
  width: 500px;
  overflow: hidden;
  border: 1px solid #333;
}

.window-header {
  background: #252526;
  padding: 10px 15px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}
.dot.red { background: #ff5f56; }
.dot.yellow { background: #ffbd2e; }
.dot.green { background: #27c93f; }
.window-header .title {
  margin-left: 15px;
  color: #888;
  font-family: monospace;
  font-size: 12px;
}

.code-window pre {
  padding: 20px;
  margin: 0;
  color: #d4d4d4;
  font-family: 'Fira Code', monospace;
  font-size: 14px;
  line-height: 1.5;
  overflow-x: auto;
}

.keyword { color: #c586c0; }
.string { color: #ce9178; }
.comment { color: #6a9955; font-style: italic; }

.docs-text h3 {
  font-size: 2.5rem;
  margin-bottom: 20px;
}
.docs-text p {
  color: #ccc;
  margin-bottom: 30px;
  font-size: 1.1rem;
  line-height: 1.6;
  max-width: 400px;
}

/* About Section */
.about-section {
  padding: 120px 20px;
  text-align: center;
  background: rgba(255,255,255,0.02);
}

.about-content h2 {
  font-size: 3rem;
  margin-bottom: 30px;
}

.mission {
  font-size: 1.5rem;
  color: #aaa;
  max-width: 800px;
  margin: 0 auto 80px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 40px;
  max-width: 1000px;
  margin: 0 auto;
}

.stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stat .number {
  font-size: 4rem;
  font-weight: 700;
  background: linear-gradient(135deg, #fff 0%, #444 100%);
  background-clip: text;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin-bottom: 10px;
}

.stat .label {
  color: #666;
  font-size: 1.1rem;
  text-transform: uppercase;
  letter-spacing: 2px;
}

/* Smooth Scroll */
html {
  scroll-behavior: smooth;
}

@media (max-width: 900px) {
  .code-preview {
    flex-direction: column;
  }
  .code-window {
    width: 100%;
  }
  .stats-grid {
    grid-template-columns: 1fr;
    gap: 60px;
  }
}
</style>
