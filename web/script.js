document.addEventListener('DOMContentLoaded', () => {
    gsap.registerPlugin(ScrollTrigger);

    // Parallax effect
    gsap.to(".layer-back", {
        scrollTrigger: {
            scrub: 1
        },
        y: (i, target) => -ScrollTrigger.maxScroll(window) * target.dataset.speed,
        ease: "none"
    });

    // --- Existing animations from the original file, with enhancements ---

    // 1. Mouse-based interaction for hero section
    const hero = document.querySelector('#hero');
    if (hero) {
        hero.addEventListener('mousemove', (e) => {
            const { clientX, clientY } = e;
            const { offsetWidth, offsetHeight } = hero;

            const xPos = (clientX / offsetWidth) - 0.5;
            const yPos = (clientY / offsetHeight) - 0.5;

            // Make the effect more dramatic
            gsap.to('.hero-title, .hero-text', {
                duration: 0.7,
                rotationY: xPos * 20,
                rotationX: yPos * 20,
                x: xPos * 30,
                y: yPos * 30,
                ease: 'power2.out'
            });
        });
    }

    // 2. Randomized, more frequent "supernatural events"
    function randomJolt() {
        const joltType = Math.random();
        if (joltType < 0.3) {
            // More intense screen shake
            gsap.fromTo('body', { x: 0, y: 0 }, { duration: 0.3, x: Math.random() * 8 - 4, y: Math.random() * 8 - 4, yoyo: true, repeat: 3, ease: 'power1.inOut' });
        } else if (joltType < 0.6) {
            // Quick flash of red light
            const flash = document.createElement('div');
            flash.style.position = 'fixed';
            flash.style.top = '0';
            flash.style.left = '0';
            flash.style.width = '100vw';
            flash.style.height = '100vh';
            flash.style.backgroundColor = 'rgba(255, 0, 0, 0.2)';
            flash.style.zIndex = '9999';
            flash.style.pointerEvents = 'none';
            document.body.appendChild(flash);

            gsap.to(flash, {
                duration: 0.15,
                opacity: 0,
                onComplete: () => {
                    document.body.removeChild(flash);
                }
            });
        } else {
            // A random feature card "jumps"
            const cards = gsap.utils.toArray('.feature-card');
            const randomCard = cards[Math.floor(Math.random() * cards.length)];
            gsap.fromTo(randomCard, { y: 0 }, { duration: 0.2, y: -15, yoyo: true, repeat: 1, ease: 'power2.inOut' });
        }
    }

    // Increased frequency of jolts
    setInterval(randomJolt, Math.random() * (8000 - 4000) + 4000); // Between 4-8 seconds

    // 3. Animate feature cards on scroll with more flair
    gsap.utils.toArray('.feature-card').forEach((card, i) => {
        gsap.from(card, {
            scrollTrigger: {
                trigger: card,
                start: 'top 90%',
                toggleActions: 'play none none none'
            },
            opacity: 0,
            y: 100,
            rotation: i % 2 === 0 ? -15 : 15,
            duration: 1,
            ease: 'power3.out'
        });
    });

    // Add a floating animation to the feature cards after they appear
    gsap.utils.toArray('.feature-card').forEach(card => {
        gsap.to(card, {
            y: '+=10',
            repeat: -1,
            yoyo: true,
            ease: 'sine.inOut',
            duration: 2,
            delay: Math.random() * 2 // Stagger the start times
        });
    });


    // --- Add even more animations ---

    // Animate the main title letters
    const mainTitle = document.querySelector('.main-title');
    if (mainTitle) {
        const text = mainTitle.innerText;
        mainTitle.innerHTML = '';
        text.split('').forEach(char => {
            const span = document.createElement('span');
            span.innerText = char;
            span.style.display = 'inline-block';
            mainTitle.appendChild(span);
        });

        gsap.from(mainTitle.children, {
            opacity: 0,
            y: -50,
            stagger: 0.05,
            delay: 1,
            ease: 'back.out(1.7)'
        });
    }

    // Animate the subtitle
    gsap.from('.subtitle', {
        opacity: 0,
        delay: 2,
        duration: 2,
        ease: 'power2.inOut'
    });

    // Make the background crackle
    gsap.to('.layer-back', {
        opacity: '+=0.1',
        repeat: -1,
        yoyo: true,
        ease: 'sine.inOut',
        duration: 5
    });

    // Ghostly text effect
    function showGhostlyText() {
        const ghostlyTexts = ["Help me", "Get out", "It burns", "The power of Christ compels you", "Demon", "Exorcist"];
        const text = ghostlyTexts[Math.floor(Math.random() * ghostlyTexts.length)];
        const ghostlyTextElement = document.createElement('div');
        ghostlyTextElement.innerText = text;
        ghostlyTextElement.style.position = 'fixed';
        ghostlyTextElement.style.top = `${Math.random() * 100}vh`;
        ghostlyTextElement.style.left = `${Math.random() * 100}vw`;
        ghostlyTextElement.style.color = 'rgba(255, 0, 0, 0.5)';
        ghostlyTextElement.style.fontSize = `${Math.random() * 2 + 1}rem`;
        ghostlyTextElement.style.fontFamily = "'Creepster', cursive";
        ghostlyTextElement.style.zIndex = '9999';
        ghostlyTextElement.style.pointerEvents = 'none';
        document.body.appendChild(ghostlyTextElement);

        gsap.to(ghostlyTextElement, {
            duration: Math.random() * 3 + 2,
            opacity: 0,
            onComplete: () => {
                document.body.removeChild(ghostlyTextElement);
            }
        });
    }
    setInterval(showGhostlyText, Math.random() * (5000 - 2000) + 2000); // Between 2-5 seconds

    // Animate footer on scroll
    gsap.from("footer p", {
        scrollTrigger: {
            trigger: "footer",
            start: "top 95%",
            toggleActions: "play none none none"
        },
        opacity: 0,
        y: 20,
        duration: 1
    });

});
