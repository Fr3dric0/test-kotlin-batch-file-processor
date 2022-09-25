from faker import Faker

FAKER = Faker()

NUMBER_OF_ENTRIES = 4_000_000


def main():
    nodes = []
    for idx in range(NUMBER_OF_ENTRIES):
        print(f'{idx}: Generating person...')
        nodes.append(f"""
        <person>
            <ssn>{FAKER.ssn()}</ssn>
            <job>
                <company>{FAKER.company()}</company>
                <position>{FAKER.job()}</position>
            </job>
            <name>{FAKER.name()}</name>
            <phone>{FAKER.phone_number()}</phone>
            <address>{FAKER.address()}</address>
            <country>{FAKER.bank_country()}</country>
            
            <!-- First card -->
            <card>
                <expires>{FAKER.credit_card_expire()}</expires>
                <number>{FAKER.credit_card_number()}</number>
                <provider>{FAKER.credit_card_provider()}</provider>
                <cvc>{FAKER.credit_card_security_code()}</cvc>
            </card>
            <!-- Second card -->
            <card>
                <expires>{FAKER.credit_card_expire()}</expires>
                <number>{FAKER.credit_card_number()}</number>
                <provider>{FAKER.credit_card_provider()}</provider>
                <cvc>{FAKER.credit_card_security_code()}</cvc>
            </card>
        </person>
        """)

    root = f"""
    <?xml version="1.0"?>
    <root>
        <metadata>
            <uploaded time="2022-10-09T12:10:00.000Z"/>
        </metadata>
        <data>
            {''.join(nodes)}
        </data>
    </root>
    """.strip()

    with open('./batch_persons_massive.xml', 'w') as f:
        f.write(root)


if __name__ == '__main__':
    main()
