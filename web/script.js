document.addEventListener('DOMContentLoaded', () => {
    const tl = gsap.timeline();

    // 1. Animate the main title
    tl.from('.main-title', {
        duration: 1.5,
        opacity: 0,
        y: -100,
        ease: 'bounce'
    });

    // 2. Animate the quote and author
    tl.from('.quote', {
        duration: 1,
        opacity: 0,
        x: -50,
        ease: 'power2.out'
    }, "-=0.5");

    tl.from('.author', {
        duration: 1,
        opacity: 0,
        x: 50,
        ease: 'power2.out'
    }, "-=0.5");

    // 3. Animate the possession meter
    tl.to('.meter-bar', {
        duration: 5,
        width: '100%',
        ease: 'power1.inOut'
    });

    // 4. Animate the footer text
    tl.from('.footer-text', {
        duration: 1.5,
        opacity: 0,
        y: 50,
        ease: 'elastic.out(1, 0.3)'
    });

    // 5. Mouse-based interaction
    document.addEventListener('mousemove', (e) => {
        const { clientX, clientY } = e;
        const { innerWidth, innerHeight } = window;

        const xPos = (clientX / innerWidth) - 0.5;
        const yPos = (clientY / innerHeight) - 0.5;

        gsap.to('.container', {
            duration: 0.5,
            rotationY: xPos * 10,
            rotationX: yPos * 10,
            ease: 'power2.out'
        });
    });

    // 6. Randomized, startling animations
    function randomJolt() {
        const joltType = Math.random();
        if (joltType < 0.3) {
            // Screen shake
            gsap.fromTo('body', { x: 0 }, { duration: 0.2, x: 10, yoyo: true, repeat: 3, ease: 'power1.inOut' });
        } else if (joltType < 0.6) {
            // Red flash
            gsap.to('body', { duration: 0.1, backgroundColor: '#ff0000', yoyo: true, repeat: 1 });
        } else {
            // Quick text scramble (pseudo-effect)
            const originalText = document.querySelector('.main-title').innerText;
            document.querySelector('.main-title').innerText = "DIABOLUS";
            setTimeout(() => {
                document.querySelector('.main-title').innerText = originalText;
            }, 150);
        }
    }

    setInterval(randomJolt, Math.random() * (10000 - 5000) + 5000); // Between 5-10 seconds
});
