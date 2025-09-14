document.addEventListener('DOMContentLoaded', () => {
    // 1. Mouse-based interaction for hero section
    const hero = document.querySelector('#hero');
    if (hero) {
        hero.addEventListener('mousemove', (e) => {
            const { clientX, clientY } = e;
            const { offsetWidth, offsetHeight } = hero;

            const xPos = (clientX / offsetWidth) - 0.5;
            const yPos = (clientY / offsetHeight) - 0.5;

            gsap.to('.hero-content', {
                duration: 0.5,
                rotationY: xPos * 10,
                rotationX: yPos * 10,
                ease: 'power2.out'
            });
        });
    }

    // 2. Randomized, subtle animations for the page
    function randomJolt() {
        const joltType = Math.random();
        if (joltType < 0.5) {
            // Subtle screen shake
            gsap.fromTo('body', { x: 0, y: 0 }, { duration: 0.2, x: Math.random() * 4 - 2, y: Math.random() * 4 - 2, yoyo: true, repeat: 2, ease: 'power1.inOut' });
        } else {
            // Quick flash of light
            const flash = document.createElement('div');
            flash.style.position = 'fixed';
            flash.style.top = '0';
            flash.style.left = '0';
            flash.style.width = '100vw';
            flash.style.height = '100vh';
            flash.style.backgroundColor = 'rgba(255, 255, 255, 0.1)';
            flash.style.zIndex = '9999';
            flash.style.pointerEvents = 'none';
            document.body.appendChild(flash);

            gsap.to(flash, {
                duration: 0.1,
                opacity: 0,
                onComplete: () => {
                    document.body.removeChild(flash);
                }
            });
        }
    }

    setInterval(randomJolt, Math.random() * (15000 - 8000) + 8000); // Between 8-15 seconds

    // 3. Animate feature cards on scroll
    gsap.utils.toArray('.feature-card').forEach(card => {
        gsap.from(card, {
            scrollTrigger: {
                trigger: card,
                start: 'top 80%',
                toggleActions: 'play none none none'
            },
            opacity: 0,
            y: 50,
            duration: 0.8,
            ease: 'power3.out'
        });
    });

    // 4. Animate workflow steps on scroll
    gsap.utils.toArray('.step').forEach(step => {
        gsap.from(step, {
            scrollTrigger: {
                trigger: step,
                start: 'top 80%',
                toggleActions: 'play none none none'
            },
            opacity: 0,
            x: -50,
            duration: 1,
            stagger: 0.2,
            ease: 'power3.out'
        });
    });
});
